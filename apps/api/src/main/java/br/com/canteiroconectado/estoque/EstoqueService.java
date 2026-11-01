package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.item.ItemResponse;
import br.com.canteiroconectado.item.ItemService;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.security.Permissoes;

/**
 * Consultas de estoque por canteiro. Os dois perfis leem; o ALMOXARIFE só o próprio canteiro.
 */
@Service
public class EstoqueService {

    static final int ULTIMAS_MOVIMENTACOES = 10;

    private final EstoqueConsultaRepository consultas;
    private final MovimentacaoConsultaRepository movimentacoes;
    private final SaldoEstoqueRepository saldos;
    private final CanteiroService canteiros;
    private final ItemService itens;

    public EstoqueService(EstoqueConsultaRepository consultas, MovimentacaoConsultaRepository movimentacoes,
            SaldoEstoqueRepository saldos, CanteiroService canteiros, ItemService itens) {
        this.consultas = consultas;
        this.movimentacoes = movimentacoes;
        this.saldos = saldos;
        this.canteiros = canteiros;
        this.itens = itens;
    }

    /**
     * Detalhe do item no canteiro com saldo, mínimo, status e as 10 últimas movimentações.
     *
     * @throws BusinessException {@code ACESSO_NEGADO} ou {@code RECURSO_NAO_ENCONTRADO}
     */
    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public DetalheEstoqueResponse detalhar(UUID canteiroId, UUID itemId) {
        canteiros.buscar(canteiroId);
        ItemResponse item = itens.buscar(itemId);
        BigDecimal saldo = saldos.buscar(canteiroId, itemId).map(SaldoEstoque::getQuantidade).orElse(BigDecimal.ZERO);
        return new DetalheEstoqueResponse(item.id(), item.codigo(), item.descricao(), item.unidade(),
            item.unidadeSimbolo(), item.tipo(), item.ativo(), item.quantidadeMinima(), Quantidades.normalizar(saldo),
            StatusEstoque.de(saldo, item.quantidadeMinima()),
            movimentacoes.ultimas(canteiroId, itemId, ULTIMAS_MOVIMENTACOES));
    }

    /**
     * Lista os itens ativos do catálogo com o saldo no canteiro, inclusive os que nunca tiveram
     * movimentação (saldo 0). Ordem: zerados, abaixo do mínimo e normais; por descrição dentro de
     * cada grupo.
     *
     * @param status filtra pelos status informados; vazio traz todos
     */
    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public PaginaResponse<ItemEstoqueResponse> listar(UUID canteiroId, String busca, List<StatusEstoque> status,
            TipoItem tipo, Integer pagina, Integer tamanho) {
        canteiros.buscar(canteiroId);
        Pageable paginacao = Paginacao.de(pagina, tamanho);
        String termo = Paginacao.termoDeBusca(busca);
        Set<StatusEstoque> filtro = status == null || status.isEmpty()
            ? EnumSet.allOf(StatusEstoque.class)
            : EnumSet.copyOf(status);

        long total = consultas.contar(canteiroId, termo, tipo, filtro);
        List<ItemEstoqueResponse> linhas = consultas.listar(canteiroId, termo, tipo, filtro,
            paginacao.getPageSize(), paginacao.getOffset());
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(linhas, paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }
}
