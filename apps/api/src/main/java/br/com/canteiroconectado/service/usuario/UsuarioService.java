package br.com.canteiroconectado.service.usuario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.dominio.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.dominio.usuario.Perfil;
import br.com.canteiroconectado.dominio.usuario.Usuario;
import br.com.canteiroconectado.dto.request.AtualizarUsuarioRequest;
import br.com.canteiroconectado.dto.request.CriarUsuarioRequest;
import br.com.canteiroconectado.dto.response.CredencialInicialResponse;
import br.com.canteiroconectado.dto.response.PaginaResponse;
import br.com.canteiroconectado.dto.response.UsuarioResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.usuario.UsuarioRepository;
import br.com.canteiroconectado.repository.usuario.UsuarioSpecifications;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.service.auditoria.AuditoriaService;
import br.com.canteiroconectado.service.canteiro.CanteiroService;
import br.com.canteiroconectado.util.Paginacao;

@Service
public class UsuarioService {

    static final String ENTIDADE = "usuario";
    private static final Sort POR_NOME = Sort.by("nome");

    private final UsuarioRepository usuarios;
    private final CanteiroService canteiros;
    private final PasswordEncoder encoderSenha;
    private final SenhaTemporariaGenerator gerador;
    private final SenhaTemporariaCipher cifrador;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, CanteiroService canteiros, PasswordEncoder encoderSenha,
            SenhaTemporariaGenerator gerador, SenhaTemporariaCipher cifrador, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.canteiros = canteiros;
        this.encoderSenha = encoderSenha;
        this.gerador = gerador;
        this.cifrador = cifrador;
        this.auditoria = auditoria;
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String busca, Perfil perfil, Boolean ativo, Integer pagina,
            Integer tamanho) {
        return PaginaResponse.of(
            usuarios.findAll(UsuarioSpecifications.filtrar(busca, perfil, ativo), Paginacao.of(pagina, tamanho, POR_NOME)),
            UsuarioResponse::of);
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UUID id) {
        return UsuarioResponse.of(encontrar(id));
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw emailDuplicado();
        }
        UUID canteiroId = canteiroPerfil(requisicao.perfil(), requisicao.canteiroId());

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
        return UsuarioResponse.of(usuario);
    }

    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public UsuarioResponse atualizar(UUID id, AtualizarUsuarioRequest requisicao) {
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
        return UsuarioResponse.of(usuario);
    }

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
        return UsuarioResponse.of(usuario);
    }

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
        return UsuarioResponse.of(usuario);
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
        return UsuarioResponse.of(usuario);
    }

    private UUID novoCanteiro(Perfil perfil, UUID pedido, UUID atual) {
        if (perfil == Perfil.GESTOR) {
            return null;
        }
        if (pedido != null && !pedido.equals(atual)) {
            return canteiroPerfil(perfil, pedido);
        }
        return atual != null ? atual : canteiroPerfil(perfil, null);
    }

    private UUID canteiroPerfil(Perfil perfil, UUID canteiroId) {
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
