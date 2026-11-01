package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro de uma entrada ou saída pelo app.
 *
 * @param id UUID gerado no app ao abrir o formulário; repetir o envio com o mesmo id não duplica
 * @param quantidade sempre positiva; o tipo diz se soma ou subtrai
 * @param frenteObra texto curto da frente de obra
 * @param observacao nota fiscal, fornecedor ou motivo do ajuste
 */
public record MovimentacaoRequest(
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
