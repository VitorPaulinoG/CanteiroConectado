package br.com.canteiroconectado.canteiro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

@Service
public class CanteiroService {

    static final String ENTIDADE = "canteiro";
    private static final Sort POR_NOME = Sort.by("nome");

    private final CanteiroRepository canteiros;
    private final CanteiroAcessoService acesso;
    private final AuditoriaService auditoria;

    public CanteiroService(CanteiroRepository canteiros, CanteiroAcessoService acesso,
            AuditoriaService auditoria) {
        this.canteiros = canteiros;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    /**
     * Lista os canteiros que o usuário logado enxerga, em ordem de nome: o ALMOXARIFE vê só o
     * próprio; o GESTOR vê todos.
     *
     * @param ativo filtra pela situação; nulo traz todos
     */
    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public List<CanteiroResponse> listar(Boolean ativo) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        List<Canteiro> encontrados = ativo == null ? canteiros.findAll(POR_NOME) : canteiros.findByAtivo(ativo, POR_NOME);
        return encontrados.stream()
            .filter(canteiro -> acesso.pode(usuario, canteiro.getId()))
            .map(CanteiroResponse::de)
            .toList();
    }

    /**
     * Devolve um canteiro a que o usuário logado tem acesso.
     *
     * @throws BusinessException {@code ACESSO_NEGADO} ou {@code RECURSO_NAO_ENCONTRADO}
     */
    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public CanteiroResponse buscar(UUID id) {
        acesso.conferir(id);
        return CanteiroResponse.de(encontrar(id));
    }

    /**
     * Cria um canteiro ativo e registra {@code CANTEIRO_CRIADO}.
     *
     * @throws BusinessException {@code CANTEIRO_DUPLICADO}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public CanteiroResponse criar(CanteiroRequest requisicao) {
        String nome = requisicao.nome().strip();
        if (canteiros.existsByNomeIgnoreCase(nome)) {
            throw nomeDuplicado();
        }
        Canteiro canteiro = canteiros.save(new Canteiro(nome, textoOpcional(requisicao.endereco())));
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.CANTEIRO_CRIADO, ENTIDADE,
            canteiro.getId(), Map.of("nome", nome));
        return CanteiroResponse.de(canteiro);
    }

    /**
     * Altera os campos informados e registra {@code CANTEIRO_EDITADO} com o que mudou.
     *
     * @throws BusinessException {@code CANTEIRO_DUPLICADO} ou {@code RECURSO_NAO_ENCONTRADO}
     */
    @PreAuthorize(Permissoes.ADMIN)
    @Transactional
    public CanteiroResponse atualizar(UUID id, CanteiroAtualizacaoRequest requisicao) {
        Canteiro canteiro = encontrar(id);
        Map<String, Object> mudancas = new LinkedHashMap<>();

        if (requisicao.nome() != null) {
            String nome = requisicao.nome().strip();
            if (!nome.equals(canteiro.getNome())) {
                if (canteiros.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
                    throw nomeDuplicado();
                }
                mudancas.put("nome", nome);
                canteiro.setNome(nome);
            }
        }
        if (requisicao.endereco() != null) {
            String endereco = textoOpcional(requisicao.endereco());
            if (!Objects.equals(endereco, canteiro.getEndereco())) {
                mudancas.put("endereco", endereco == null ? "" : endereco);
                canteiro.setEndereco(endereco);
            }
        }
        if (requisicao.ativo() != null && requisicao.ativo() != canteiro.isAtivo()) {
            mudancas.put("ativo", requisicao.ativo());
            canteiro.setAtivo(requisicao.ativo());
        }

        if (!mudancas.isEmpty()) {
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.CANTEIRO_EDITADO, ENTIDADE,
                canteiro.getId(), mudancas);
        }
        return CanteiroResponse.de(canteiro);
    }

    /**
     * Devolve o canteiro se ele existe e está ativo; usado por quem grava dados num canteiro.
     *
     * @throws BusinessException {@code RECURSO_NAO_ENCONTRADO} ou {@code CANTEIRO_INATIVO}
     */
    @Transactional(readOnly = true)
    public Canteiro exigirAtivo(UUID id) {
        Canteiro canteiro = encontrar(id);
        if (!canteiro.isAtivo()) {
            throw new BusinessException(ErrorCode.CANTEIRO_INATIVO,
                "O canteiro " + canteiro.getNome() + " está desativado.");
        }
        return canteiro;
    }

    private Canteiro encontrar(UUID id) {
        return canteiros.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Canteiro não encontrado."));
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static BusinessException nomeDuplicado() {
        return new BusinessException(ErrorCode.CANTEIRO_DUPLICADO, "Já existe um canteiro com este nome.")
            .com("campo", "nome");
    }
}
