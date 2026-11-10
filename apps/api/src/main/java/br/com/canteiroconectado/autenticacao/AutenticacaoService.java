package br.com.canteiroconectado.autenticacao;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.JwtService;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@Service
public class AutenticacaoService {

    static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoderSenha;
    private final JwtService jwtService;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder encoderSenha,
            JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoderSenha = encoderSenha;
        this.jwtService = jwtService;
    }

    /**
     * Valida e-mail e senha e devolve um JWT com os dados do usuário. E-mail inexistente e senha
     * errada recebem a mesma resposta ({@code CREDENCIAIS_INVALIDAS}) para não revelar quais
     * e-mails existem.
     *
     * @throws BusinessException {@code CREDENCIAIS_INVALIDAS} ou {@code USUARIO_INATIVO}
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest requisicao) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(Usuario.normalizarEmail(requisicao.email()))
            .filter(encontrado -> encoderSenha.matches(requisicao.senha(), encontrado.getSenhaHash()))
            .orElseThrow(() -> new BusinessException(ErrorCode.CREDENCIAIS_INVALIDAS,
                MENSAGEM_CREDENCIAIS_INVALIDAS));

        if (!usuario.isAtivo()) {
            throw new BusinessException(ErrorCode.USUARIO_INATIVO,
                "Este usuário está desativado. Fale com o administrador.");
        }

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
}
