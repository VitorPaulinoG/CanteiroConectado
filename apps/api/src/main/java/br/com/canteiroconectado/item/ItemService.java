package br.com.canteiroconectado.item;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

/**
 * Catálogo de itens. ALMOXARIFE e GESTOR escrevem; todos leem. Itens nunca são excluídos.
 */
@Service
public class ItemService {

    static final String ENTIDADE = "item";

    private final ItemRepository itens;
    private final List<UsoDoItem> usos;
    private final AuditoriaService auditoria;

    public ItemService(ItemRepository itens, List<UsoDoItem> usos, AuditoriaService auditoria) {
        this.itens = itens;
        this.usos = usos;
        this.auditoria = auditoria;
    }

    /**
     * Lista o catálogo em ordem de descrição, com filtro por tipo e situação.
     */
    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public PaginaResponse<ItemResponse> listar(TipoItem tipo, Boolean ativo, Integer pagina, Integer tamanho) {
        return PaginaResponse.de(itens.listar(tipo, ativo, Paginacao.de(pagina, tamanho)), ItemResponse::de);
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public ItemResponse buscar(UUID id) {
        return ItemResponse.de(encontrar(id));
    }

    /**
     * Cadastra um item ativo e registra {@code ITEM_CRIADO}.
     *
     * @throws BusinessException {@code CODIGO_DUPLICADO}
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ItemResponse criar(ItemRequest requisicao) {
        String codigo = requisicao.codigo().strip();
        if (itens.existsByCodigoIgnoreCase(codigo)) {
            throw codigoDuplicado();
        }
        BigDecimal minimo = requisicao.quantidadeMinima() == null ? BigDecimal.ZERO : requisicao.quantidadeMinima();
        Item item = itens.save(new Item(codigo, requisicao.descricao().strip(), requisicao.unidade(),
            requisicao.tipo(), minimo));
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.ITEM_CRIADO, ENTIDADE, item.getId(),
            Map.of("codigo", codigo, "descricao", item.getDescricao()));
        return ItemResponse.de(item);
    }

    /**
     * Altera os campos informados e registra {@code ITEM_EDITADO} com o que mudou.
     *
     * @throws BusinessException {@code CODIGO_DUPLICADO} ou {@code ITEM_JA_MOVIMENTADO} (código ou
     *     unidade de item que já teve movimentação)
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ItemResponse atualizar(UUID id, ItemAtualizacaoRequest requisicao) {
        Item item = encontrar(id);
        Map<String, Object> mudancas = new LinkedHashMap<>();

        String codigo = requisicao.codigo() == null ? null : requisicao.codigo().strip();
        boolean mudaCodigo = codigo != null && !codigo.equals(item.getCodigo());
        boolean mudaUnidade = requisicao.unidade() != null && requisicao.unidade() != item.getUnidade();
        if ((mudaCodigo || mudaUnidade) && jaMovimentado(id)) {
            throw new BusinessException(ErrorCode.ITEM_JA_MOVIMENTADO,
                "Este item já teve movimentação: o código e a unidade não podem mais mudar. "
                    + "Para corrigir, desative-o e cadastre um item novo.")
                .com("campo", mudaCodigo ? "codigo" : "unidade");
        }
        if (mudaCodigo) {
            if (itens.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
                throw codigoDuplicado();
            }
            item.setCodigo(codigo);
            mudancas.put("codigo", codigo);
        }
        if (mudaUnidade) {
            item.setUnidade(requisicao.unidade());
            mudancas.put("unidade", requisicao.unidade().name());
        }
        if (requisicao.descricao() != null && !requisicao.descricao().strip().equals(item.getDescricao())) {
            item.setDescricao(requisicao.descricao().strip());
            mudancas.put("descricao", item.getDescricao());
        }
        if (requisicao.tipo() != null && requisicao.tipo() != item.getTipo()) {
            item.setTipo(requisicao.tipo());
            mudancas.put("tipo", requisicao.tipo().name());
        }
        if (requisicao.quantidadeMinima() != null
                && requisicao.quantidadeMinima().compareTo(item.getQuantidadeMinima()) != 0) {
            item.setQuantidadeMinima(requisicao.quantidadeMinima());
            mudancas.put("quantidadeMinima", requisicao.quantidadeMinima().toPlainString());
        }
        if (requisicao.ativo() != null && requisicao.ativo() != item.isAtivo()) {
            item.setAtivo(requisicao.ativo());
            mudancas.put("ativo", requisicao.ativo());
        }

        if (!mudancas.isEmpty()) {
            auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.ITEM_EDITADO, ENTIDADE, item.getId(),
                mudancas);
        }
        return ItemResponse.de(item);
    }

    /**
     * Devolve o item se ele existe e está ativo; usado pelas movimentações e requisições.
     *
     * @throws BusinessException {@code ITEM_INATIVO} ou {@code RECURSO_NAO_ENCONTRADO}
     */
    @Transactional(readOnly = true)
    public Item exigirAtivo(UUID id) {
        Item item = encontrar(id);
        if (!item.isAtivo()) {
            throw new BusinessException(ErrorCode.ITEM_INATIVO, "O item " + item.getDescricao() + " está desativado.")
                .com("campo", "itemId");
        }
        return item;
    }

    private boolean jaMovimentado(UUID id) {
        return usos.stream().anyMatch(uso -> uso.jaMovimentado(id));
    }

    private Item encontrar(UUID id) {
        return itens.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Item não encontrado."));
    }

    private static BusinessException codigoDuplicado() {
        return new BusinessException(ErrorCode.CODIGO_DUPLICADO, "Código já cadastrado.").com("campo", "codigo");
    }
}
