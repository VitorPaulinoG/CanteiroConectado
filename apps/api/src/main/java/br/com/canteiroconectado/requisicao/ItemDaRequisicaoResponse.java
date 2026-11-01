package br.com.canteiroconectado.requisicao;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDaRequisicaoResponse(
    UUID itemId,
    String codigo,
    String descricao,
    String unidadeSimbolo,
    BigDecimal quantidade
) {
}
