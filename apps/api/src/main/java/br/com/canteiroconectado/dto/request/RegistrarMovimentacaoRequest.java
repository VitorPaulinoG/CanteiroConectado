package br.com.canteiroconectado.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.canteiroconectado.domain.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.domain.estoque.TipoMovimentacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarMovimentacaoRequest(
    String id,

    @NotNull(message = "Informe o tipo (ENTRADA ou SAIDA)")
    TipoMovimentacao tipo,

    @NotNull(message = "Informe o motivo")
    MotivoMovimentacao motivo,

    @NotNull(message = "Informe o item")
    UUID itemId,

    @NotNull(message = "Informe a quantidade")
    BigDecimal quantidade,

    UUID colaboradorId,

    @Size(max = 60, message = "A frente de obra deve ter no máximo 60 caracteres")
    String frenteObra,

    @Size(max = 200, message = "A observação deve ter no máximo 200 caracteres")
    String observacao
) {
}
