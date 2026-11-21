package br.com.canteiroconectado.usuario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

/**
 * Cadastro de funcionários, usado só pelo administrador. Funcionários nunca são excluídos.
 * ALMOXARIFE pertence a um canteiro ativo; GESTOR não tem canteiro. A senha temporária é
 * guardada cifrada até a primeira troca, para o administrador poder consultá-la.
 */
@Service
public class UsuarioService {

    static final String ENTIDADE = "usuario";
    private static final Sort POR_NOME = Sort.by("nome");

    private final UsuarioRepository usuarios;
    private final CanteiroService canteiros;
    private final PasswordEncoder encoderSenha;
    private final GeradorSenhaTemporaria gerador;
    private final CifradorSenhaTemporaria cifrador;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, CanteiroService canteiros, PasswordEncoder encoderSenha,
            GeradorSenhaTemporaria gerador, CifradorSenhaTemporaria cifrador, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.canteiros = canteiros;
        this.encoderSenha = encoderSenha;
        this.gerador = gerador;
        this.cifrador = cifrador;
        this.auditoria = auditoria;
    }

    /**
     * Lista funcionários em ordem de nome, com busca por nome ou e-mail.
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String busca, Perfil perfil, Boolean ativo, Integer pagina,
            Integer tamanho) {
        return PaginaResponse.de(
            usuarios.findAll(UsuarioEspecificacoes.filtrar(busca, perfil, ativo), Paginacao.de(pagina, tamanho, POR_NOME)),
            UsuarioResponse::de);
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UUID id) {
        return UsuarioResponse.de(encontrar(id));
    }

    /**
     * Cria o funcionário com uma senha temporária de 12 caracteres e a troca obrigatória no
     * primeiro acesso. Registra {@code USUARIO_CRIADO}.
     *
     * @throws BusinessException {@code EMAIL_JA_CADASTRADO}, {@code CANTEIRO_OBRIGATORIO} ou
     *     {@code CANTEIRO_INATIVO}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse criar(UsuarioRequest requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw emailDuplicado();
        }
        UUID canteiroId = canteiroDoPerfil(requisicao.perfil(), requisicao.canteiroId());

        String senhaTemporaria = gerador.gerar();
        Usuario usuario = new Usuario(requisicao.nome().strip(), email, encoderSenha.encode(senhaTemporaria),
            requisicao.perfil(), false, canteiroId);
        usuario.setPrecisaTrocarSenha(true);
        usuario.setSenhaTemporariaCifrada(cifrador.cifrar(senhaTemporaria, usuario.getId()));
        usuarios.save(usuario);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("email", email);
        detalhes.put("perfil", usuario.getPerfil().name());
        if (canteiroId != null) {
            detalhes.put("canteiroId", canteiroId.toString());
        }
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.USUARIO_CRIADO, ENTIDADE, usuario.getId(),
            detalhes);
        return UsuarioResponse.de(usuario);
    }

    /**
     * Altera os campos informados e registra {@code USUARIO_EDITADO} com o que mudou. Mudar o
     * perfil ou o canteiro encerra a sessão atual do funcionário.
     *
     * @throws BusinessException {@code EMAIL_JA_CADASTRADO}, {@code CANTEIRO_OBRIGATORIO},
     *     {@code CANTEIRO_INATIVO} ou {@code ADMINISTRADOR_PROTEGIDO}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioAtualizacaoRequest requisicao) {
        Usuario usuario = encontrar(id);
        Map<String, Object> mudancas = new LinkedHashMap<>();

        if (requisicao.nome() != null && !requisicao.nome().strip().equals(usuario.getNome())) {
            usuario.setNome(requisicao.nome().strip());
            mudancas.put("nome", usuario.getNome());
        }
        if (requisicao.email() != null) {
            String email = Usuario.normalizarEmail(requisicao.email());
            if (!email.equals(usuario.getEmail())) {
                if (usuarios.existsByEmailIgnoreCaseAndIdNot(email, id)) {
                    throw emailDuplicado();
                }
                usuario.setEmail(email);
                mudancas.put("email", email);
            }
        }

        Perfil perfil = requisicao.perfil() != null ? requisicao.perfil() : usuario.getPerfil();
        if (usuario.isAdministrador() && perfil != usuario.getPerfil()) {
            throw new BusinessException(ErrorCode.ADMINISTRADOR_PROTEGIDO,
                "O perfil do administrador não pode ser alterado.");
        }
        UUID canteiroId = novoCanteiro(perfil, requisicao.canteiroId(), usuario.getCanteiroId());
        if (perfil != usuario.getPerfil()) {
            mudancas.put("perfil", perfil.name());
        }
        if (!Objects.equals(canteiroId, usuario.getCanteiroId())) {
            mudancas.put("canteiroId", canteiroId == null ? "" : canteiroId.toString());
        }
        usuario.setPerfil(perfil);
        usuario.setCanteiroId(canteiroId);

        if (!mudancas.isEmpty()) {
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.USUARIO_EDITADO, ENTIDADE, usuario.getId(),
                mudancas);
        }
        return UsuarioResponse.de(usuario);
    }

    /**
     * Gera uma nova senha temporária, exige a troca no próximo login, desfaz um bloqueio por
     * tentativas e registra {@code SENHA_REDEFINIDA}.
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse redefinirSenha(UUID id) {
        Usuario usuario = encontrar(id);
        String senhaTemporaria = gerador.gerar();
        usuario.setSenhaHash(encoderSenha.encode(senhaTemporaria));
        usuario.setPrecisaTrocarSenha(true);
        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuario.setSenhaTemporariaCifrada(cifrador.cifrar(senhaTemporaria, usuario.getId()));
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.SENHA_REDEFINIDA, ENTIDADE, usuario.getId(),
            Map.of("email", usuario.getEmail()));
        return UsuarioResponse.de(usuario);
    }

    /**
     * Devolve o e-mail e a senha temporária enquanto o funcionário não a trocou, e registra
     * {@code CREDENCIAL_CONSULTADA} com quem consultou (sem a senha).
     *
     * @throws BusinessException {@code CREDENCIAL_JA_TROCADA}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public CredencialInicialResponse credencialInicial(UUID id) {
        Usuario usuario = encontrar(id);
        if (usuario.getSenhaTemporariaCifrada() == null) {
            throw new BusinessException(ErrorCode.CREDENCIAL_JA_TROCADA,
                "O funcionário já trocou a senha temporária. Para gerar outra, redefina a senha.");
        }
        String senha = cifrador.decifrar(usuario.getSenhaTemporariaCifrada(), usuario.getId());
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.CREDENCIAL_CONSULTADA, ENTIDADE,
            usuario.getId(), Map.of("email", usuario.getEmail()));
        return new CredencialInicialResponse(usuario.getEmail(), senha);
    }

    /**
     * Desativa o funcionário: ele não entra mais e o token atual deixa de valer.
     *
     * @throws BusinessException {@code AUTODESATIVACAO_PROIBIDA}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse desativar(UUID id) {
        Usuario usuario = encontrar(id);
        if (usuario.getId().equals(UsuarioLogado.atual().id())) {
            throw new BusinessException(ErrorCode.AUTODESATIVACAO_PROIBIDA, "Você não pode desativar a si mesmo.");
        }
        if (usuario.isAtivo()) {
            usuario.setAtivo(false);
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.USUARIO_DESATIVADO, ENTIDADE,
                usuario.getId(), Map.of("email", usuario.getEmail()));
        }
        return UsuarioResponse.de(usuario);
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse ativar(UUID id) {
        Usuario usuario = encontrar(id);
        if (!usuario.isAtivo()) {
            if (usuario.getCanteiroId() != null) {
                canteiros.exigirAtivo(usuario.getCanteiroId());
            }
            usuario.setAtivo(true);
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.USUARIO_ATIVADO, ENTIDADE,
                usuario.getId(), Map.of("email", usuario.getEmail()));
        }
        return UsuarioResponse.de(usuario);
    }

    private UUID novoCanteiro(Perfil perfil, UUID pedido, UUID atual) {
        if (perfil == Perfil.GESTOR) {
            return null;
        }
        if (pedido != null && !pedido.equals(atual)) {
            return canteiroDoPerfil(perfil, pedido);
        }
        return atual != null ? atual : canteiroDoPerfil(perfil, null);
    }

    private UUID canteiroDoPerfil(Perfil perfil, UUID canteiroId) {
        if (perfil == Perfil.GESTOR) {
            return null;
        }
        if (canteiroId == null) {
            throw new BusinessException(ErrorCode.CANTEIRO_OBRIGATORIO, "O almoxarife precisa de um canteiro.")
                .com("campo", "canteiroId");
        }
        return canteiros.exigirAtivo(canteiroId).getId();
    }

    private Usuario encontrar(UUID id) {
        return usuarios.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Funcionário não encontrado."));
    }

    private static BusinessException emailDuplicado() {
        return new BusinessException(ErrorCode.EMAIL_JA_CADASTRADO, "Já existe um funcionário com este e-mail.")
            .com("campo", "email");
    }
}
