package br.com.canteiroconectado.autenticacao;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.JwtService;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@Service
public class AutenticacaoService {

    static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";
    static final int MAXIMO_TENTATIVAS = 5;
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);
    static final String ENTIDADE = "usuario";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoderSenha;
    private final JwtService jwtService;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder encoderSenha,
            JwtService jwtService, AuditoriaService auditoria, Clock relogio) {
        this.usuarios = usuarios;
        this.encoderSenha = encoderSenha;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.hashFicticio = encoderSenha.encode(UUID.randomUUID().toString());
    }

    /**
     * Valida e-mail e senha e devolve um JWT com os dados do usuário.
     *
     * <p>E-mail inexistente e senha errada recebem a mesma resposta para não revelar quais e-mails
     * existem. A quinta senha errada seguida bloqueia a conta por 15 minutos; o login correto zera
     * o contador. Cada tentativa vira evento em {@code log_auditoria}, inclusive as que falham, por
     * isso a transação não é desfeita quando a regra recusa o login.</p>
     *
     * @throws BusinessException {@code CREDENCIAIS_INVALIDAS}, {@code CONTA_BLOQUEADA} (423, com
     *     {@code bloqueadoAte}) ou {@code USUARIO_INATIVO}
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(email);
        if (encontrado.isEmpty()) {
            encoderSenha.matches(requisicao.senha(), hashFicticio);
            registrarFalha(null, email, "EMAIL_INEXISTENTE");
            throw credenciaisInvalidas();
        }

        Usuario usuario = encontrado.get();
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        if (estaBloqueado(usuario, agora)) {
            registrarFalha(usuario.getId(), email, "CONTA_BLOQUEADA");
            throw contaBloqueada(usuario.getBloqueadoAte(), agora);
        }

        if (!encoderSenha.matches(requisicao.senha(), usuario.getSenhaHash())) {
            contarFalha(usuario, email, agora);
            throw estaBloqueado(usuario, agora)
                ? contaBloqueada(usuario.getBloqueadoAte(), agora)
                : credenciaisInvalidas();
        }

        if (!usuario.isAtivo()) {
            registrarFalha(usuario.getId(), email, "USUARIO_INATIVO");
            throw new BusinessException(ErrorCode.USUARIO_INATIVO,
                "Este usuário está desativado. Fale com o administrador.");
        }

        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        auditoria.registrar(usuario.getId(), AcaoAuditoria.LOGIN_SUCESSO, ENTIDADE, usuario.getId(), null);

        JwtService.TokenGerado token = jwtService.gerarToken(usuario);
        return new LoginResponse(token.valor(), token.expiraEm(), UsuarioLogadoResponse.de(usuario));
    }

    /**
     * Devolve os dados do usuário identificado pelo {@code sub} do token.
     *
     * @throws BusinessException {@code NAO_AUTENTICADO} se o usuário não existe mais
     */
    @Transactional(readOnly = true)
    public UsuarioLogadoResponse buscarLogado(UUID usuarioId) {
        return usuarios.findById(usuarioId)
            .map(UsuarioLogadoResponse::de)
            .orElseThrow(() -> new BusinessException(ErrorCode.NAO_AUTENTICADO, "Sessão inválida."));
    }

    /**
     * Troca a senha do usuário logado. Apaga a senha temporária cifrada e a marca de troca
     * obrigatória e registra {@code SENHA_TROCADA}, sem guardar senha alguma no evento.
     *
     * @throws BusinessException {@code SENHA_ATUAL_INCORRETA}, {@code SENHA_FRACA} ou
     *     {@code SENHA_IGUAL_A_ATUAL}
     */
    @Transactional
    public UsuarioLogadoResponse trocarSenha(UUID usuarioId, TrocarSenhaRequest requisicao) {
        Usuario usuario = usuarios.findById(usuarioId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NAO_AUTENTICADO, "Sessão inválida."));

        if (!encoderSenha.matches(requisicao.senhaAtual(), usuario.getSenhaHash())) {
            throw new BusinessException(ErrorCode.SENHA_ATUAL_INCORRETA, "A senha atual está incorreta.");
        }
        PoliticaDeSenha.problemaDa(requisicao.novaSenha()).ifPresent(problema -> {
            throw new BusinessException(ErrorCode.SENHA_FRACA, problema);
        });
        if (requisicao.novaSenha().equals(requisicao.senhaAtual())) {
            throw new BusinessException(ErrorCode.SENHA_IGUAL_A_ATUAL,
                "A nova senha deve ser diferente da atual.");
        }

        boolean eraObrigatoria = usuario.isPrecisaTrocarSenha();
        usuario.setSenhaHash(encoderSenha.encode(requisicao.novaSenha()));
        usuario.setPrecisaTrocarSenha(false);
        usuario.setSenhaTemporariaCifrada(null);
        auditoria.registrar(usuario.getId(), AcaoAuditoria.SENHA_TROCADA, ENTIDADE, usuario.getId(),
            Map.of("primeiroAcesso", eraObrigatoria));
        return UsuarioLogadoResponse.de(usuario);
    }

    private static boolean estaBloqueado(Usuario usuario, OffsetDateTime agora) {
        return usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(agora);
    }

    private void contarFalha(Usuario usuario, String email, OffsetDateTime agora) {
        int tentativas = usuario.getTentativasFalhas() + 1;
        registrarFalha(usuario.getId(), email, "SENHA_INCORRETA");
        if (tentativas >= MAXIMO_TENTATIVAS) {
            OffsetDateTime ate = agora.plus(DURACAO_BLOQUEIO);
            usuario.setTentativasFalhas(0);
            usuario.setBloqueadoAte(ate);
            auditoria.registrar(usuario.getId(), AcaoAuditoria.CONTA_BLOQUEADA, ENTIDADE, usuario.getId(),
                Map.of("bloqueadoAte", ate.toInstant().toString(), "tentativas", tentativas));
            return;
        }
        usuario.setTentativasFalhas(tentativas);
    }

    private void registrarFalha(UUID usuarioId, String email, String motivo) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("email", email);
        detalhes.put("motivo", motivo);
        auditoria.registrar(usuarioId, AcaoAuditoria.LOGIN_FALHOU, ENTIDADE, usuarioId, detalhes);
    }

    private static BusinessException credenciaisInvalidas() {
        return new BusinessException(ErrorCode.CREDENCIAIS_INVALIDAS, MENSAGEM_CREDENCIAIS_INVALIDAS);
    }

    private static BusinessException contaBloqueada(OffsetDateTime ate, OffsetDateTime agora) {
        long minutos = Math.max(1, (long) Math.ceil(Duration.between(agora, ate).getSeconds() / 60.0));
        return new BusinessException(ErrorCode.CONTA_BLOQUEADA,
            "Conta bloqueada por excesso de tentativas. Tente de novo em " + minutos
                + (minutos == 1 ? " minuto." : " minutos."))
            .com("bloqueadoAte", ate.toInstant().toString());
    }
}
