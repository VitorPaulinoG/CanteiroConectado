package br.com.canteiroconectado.requisicao;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ItemDaRequisicaoRequest(
    @NotNull(message = "Informe o item")
    UUID itemId,

    @NotNull(message = "Informe a quantidade")
    BigDecimal quantidade
) {
}
