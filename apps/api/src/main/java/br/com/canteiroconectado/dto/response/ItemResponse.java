package br.com.canteiroconectado.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.canteiroconectado.domain.item.Item;
import br.com.canteiroconectado.domain.item.TipoItem;
import br.com.canteiroconectado.domain.item.Unidade;
import br.com.canteiroconectado.util.Quantidades;

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

    public static ItemResponse of(Item item) {
        return new ItemResponse(item.getId(), item.getCodigo(), item.getDescricao(), item.getUnidade(),
            item.getUnidade().getSimbolo(), item.getTipo(), Quantidades.normalizar(item.getQuantidadeMinima()),
            item.isAtivo());
    }
}
