package br.com.canteiroconectado.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ItemRequisicaoRequest(
    @NotNull(message = "Informe o item")
    UUID itemId,

    @NotNull(message = "Informe a quantidade")
    BigDecimal quantidade
) {
}
