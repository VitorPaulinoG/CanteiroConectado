package br.com.canteiroconectado.service.item;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.dominio.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.dominio.item.Item;
import br.com.canteiroconectado.dominio.item.ItemUso;
import br.com.canteiroconectado.dominio.item.TipoItem;
import br.com.canteiroconectado.dto.request.AtualizarItemRequest;
import br.com.canteiroconectado.dto.request.CriarItemRequest;
import br.com.canteiroconectado.dto.response.ItemResponse;
import br.com.canteiroconectado.dto.response.PaginaResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.item.ItemRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.service.auditoria.AuditoriaService;
import br.com.canteiroconectado.util.Paginacao;

@Service
public class ItemService {

    static final String ENTIDADE = "item";

    private final ItemRepository itens;
    private final List<ItemUso> usos;
    private final AuditoriaService auditoria;

    public ItemService(ItemRepository itens, List<ItemUso> usos, AuditoriaService auditoria) {
        this.itens = itens;
        this.usos = usos;
        this.auditoria = auditoria;
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public PaginaResponse<ItemResponse> listar(String busca, TipoItem tipo, Boolean ativo, Integer pagina,
            Integer tamanho) {
        return PaginaResponse.of(
            itens.buscar(Paginacao.termoBusca(busca), tipo, ativo, Paginacao.of(pagina, tamanho)),
            ItemResponse::of);
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public ItemResponse buscar(UUID id) {
        return ItemResponse.of(encontrar(id));
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ItemResponse criar(CriarItemRequest requisicao) {
        String codigo = requisicao.codigo().strip();
        if (itens.existsByCodigoIgnoreCase(codigo)) {
            throw codigoDuplicado();
        }
        BigDecimal minimo = requisicao.quantidadeMinima() == null ? BigDecimal.ZERO : requisicao.quantidadeMinima();
        Item item = itens.save(new Item(codigo, requisicao.descricao().strip(), requisicao.unidade(),
            requisicao.tipo(), minimo));
        auditoria.registrar(UsuarioLogado.atual().id(), AcaoAuditoria.ITEM_CRIADO, ENTIDADE, item.getId(),
            Map.of("codigo", codigo, "descricao", item.getDescricao()));
        return ItemResponse.of(item);
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public ItemResponse atualizar(UUID id, AtualizarItemRequest requisicao) {
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
        return ItemResponse.of(item);
    }

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
