package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;

/**
 * Linha da consulta de estoque: o item do catálogo com o saldo no canteiro (0 quando ainda não
 * houve movimentação) e o status em relação ao mínimo.
 */
public record ItemEstoqueResponse(
    UUID itemId,
    String codigo,
    String descricao,
    Unidade unidade,
    String unidadeSimbolo,
    TipoItem tipo,
    BigDecimal quantidadeMinima,
    BigDecimal saldo,
    StatusEstoque status
) {
}
