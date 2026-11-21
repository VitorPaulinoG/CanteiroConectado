package br.com.canteiroconectado.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.canteiroconectado.dominio.estoque.StatusEstoque;
import br.com.canteiroconectado.dominio.item.TipoItem;
import br.com.canteiroconectado.dominio.item.Unidade;

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
