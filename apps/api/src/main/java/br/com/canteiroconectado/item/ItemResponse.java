package br.com.canteiroconectado.item;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.canteiroconectado.comum.Quantidades;

/**
 * Item do catálogo.
 *
 * @param unidadeSimbolo unidade como o usuário lê (un, kg, m², ...)
 */
public record ItemResponse(
    UUID id,
    String codigo,
    String descricao,
    Unidade unidade,
    String unidadeSimbolo,
    TipoItem tipo,
    BigDecimal quantidadeMinima,
    boolean ativo
) {

    public static ItemResponse de(Item item) {
        return new ItemResponse(item.getId(), item.getCodigo(), item.getDescricao(), item.getUnidade(),
            item.getUnidade().getSimbolo(), item.getTipo(), Quantidades.normalizar(item.getQuantidadeMinima()),
            item.isAtivo());
    }
}
