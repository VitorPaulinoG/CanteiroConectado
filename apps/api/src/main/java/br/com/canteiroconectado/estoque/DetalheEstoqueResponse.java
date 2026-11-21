package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;

/**
 * Detalhe de um item no canteiro (S07): saldo contra o mínimo e as últimas movimentações, da
 * mais recente para a mais antiga.
 *
 * @param ativo situação do item no catálogo; um item desativado continua consultável
 */
public record DetalheEstoqueResponse(
    UUID itemId,
    String codigo,
    String descricao,
    Unidade unidade,
    String unidadeSimbolo,
    TipoItem tipo,
    boolean ativo,
    BigDecimal quantidadeMinima,
    BigDecimal saldo,
    StatusEstoque status,
    List<MovimentacaoResponse> ultimasMovimentacoes
) {
}
