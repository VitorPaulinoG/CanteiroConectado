package br.com.canteiroconectado.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemRequisicaoResponse(
    UUID itemId,
    String codigo,
    String descricao,
    String unidadeSimbolo,
    BigDecimal quantidade
) {
}
