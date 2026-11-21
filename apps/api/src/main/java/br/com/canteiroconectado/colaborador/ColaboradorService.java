package br.com.canteiroconectado.colaborador;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

/**
 * Cadastro de colaboradores. O ALMOXARIFE cadastra e consulta só no próprio canteiro; o GESTOR
 * escolhe o canteiro.
 */
@Service
public class ColaboradorService {

    static final String ENTIDADE = "colaborador";

    private final ColaboradorRepository colaboradores;
    private final CanteiroService canteiros;
    private final CanteiroAcessoService acesso;
    private final AuditoriaService auditoria;

    public ColaboradorService(ColaboradorRepository colaboradores, CanteiroService canteiros,
            CanteiroAcessoService acesso, AuditoriaService auditoria) {
        this.colaboradores = colaboradores;
        this.canteiros = canteiros;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    /**
     * Lista os colaboradores do canteiro com busca por nome. Quem começa com o termo vem primeiro.
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<ColaboradorResponse> listar(UUID canteiroId, String busca, Boolean ativo, Integer pagina,
            Integer tamanho) {
        UUID canteiro = acesso.resolver(canteiroId);
        return PaginaResponse.de(
            colaboradores.buscar(canteiro, Paginacao.termoDeBusca(busca), ativo,
                Paginacao.de(pagina, tamanho, Sort.unsorted())),
            ColaboradorResponse::de);
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public ColaboradorResponse buscar(UUID id) {
        Colaborador colaborador = encontrar(id);
        acesso.conferir(colaborador.getCanteiroId());
        return ColaboradorResponse.de(colaborador);
    }

    /**
     * Cadastra um colaborador ativo e registra {@code COLABORADOR_CRIADO}.
     *
     * @throws BusinessException {@code COLABORADOR_DUPLICADO}, {@code CANTEIRO_INATIVO} ou
     *     {@code ACESSO_NEGADO}
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ColaboradorResponse criar(ColaboradorRequest requisicao) {
        UUID canteiroId = acesso.resolver(requisicao.canteiroId());
        canteiros.exigirAtivo(canteiroId);
        String nome = requisicao.nome().strip();
        if (colaboradores.existsByCanteiroIdAndNomeIgnoreCase(canteiroId, nome)) {
            throw nomeDuplicado();
        }
        Colaborador colaborador = colaboradores.save(new Colaborador(nome, canteiroId));
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.COLABORADOR_CRIADO, ENTIDADE,
            colaborador.getId(), Map.of("nome", nome, "canteiroId", canteiroId.toString()));
        return ColaboradorResponse.de(colaborador);
    }

    /**
     * Renomeia, desativa ou reativa um colaborador e registra {@code COLABORADOR_EDITADO}.
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ColaboradorResponse atualizar(UUID id, ColaboradorAtualizacaoRequest requisicao) {
        Colaborador colaborador = encontrar(id);
        acesso.conferir(colaborador.getCanteiroId());
        Map<String, Object> mudancas = new LinkedHashMap<>();

        if (requisicao.nome() != null && !requisicao.nome().strip().equals(colaborador.getNome())) {
            String nome = requisicao.nome().strip();
            if (colaboradores.existsByCanteiroIdAndNomeIgnoreCaseAndIdNot(colaborador.getCanteiroId(), nome, id)) {
                throw nomeDuplicado();
            }
            colaborador.setNome(nome);
            mudancas.put("nome", nome);
        }
        if (requisicao.ativo() != null && requisicao.ativo() != colaborador.isAtivo()) {
            colaborador.setAtivo(requisicao.ativo());
            mudancas.put("ativo", requisicao.ativo());
        }

        if (!mudancas.isEmpty()) {
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.COLABORADOR_EDITADO, ENTIDADE,
                colaborador.getId(), mudancas);
        }
        return ColaboradorResponse.de(colaborador);
    }

    /**
     * Devolve o colaborador se ele está ativo e pertence ao canteiro; usado pelas movimentações.
     *
     * @throws BusinessException {@code COLABORADOR_INVALIDO}
     */
    @Transactional(readOnly = true)
    public Colaborador exigirAtivoNoCanteiro(UUID id, UUID canteiroId) {
        Colaborador colaborador = colaboradores.findById(id)
            .orElseThrow(() -> colaboradorInvalido("Colaborador não encontrado."));
        if (!colaborador.getCanteiroId().equals(canteiroId)) {
            throw colaboradorInvalido("O colaborador não pertence a este canteiro.");
        }
        if (!colaborador.isAtivo()) {
            throw colaboradorInvalido("O colaborador " + colaborador.getNome() + " está desativado.");
        }
        return colaborador;
    }

    private Colaborador encontrar(UUID id) {
        return colaboradores.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Colaborador não encontrado."));
    }

    private static BusinessException nomeDuplicado() {
        return new BusinessException(ErrorCode.COLABORADOR_DUPLICADO,
            "Já existe um colaborador com este nome neste canteiro.").com("campo", "nome");
    }

    private static BusinessException colaboradorInvalido(String mensagem) {
        return new BusinessException(ErrorCode.COLABORADOR_INVALIDO, mensagem).com("campo", "colaboradorId");
    }
}
