package br.com.canteiroconectado.item;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alteração parcial de um item: só os campos informados mudam. {@code ativo = false} desativa.
 * Código e unidade não mudam depois da primeira movimentação.
 */
public record ItemAtualizacaoRequest(
    @Size(max = 30, message = "O código deve ter no máximo 30 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "O código não pode ficar em branco")
    String codigo,

    @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "A descrição não pode ficar em branco")
    String descricao,

    Unidade unidade,

    TipoItem tipo,

    @DecimalMin(value = "0", message = "A quantidade mínima não pode ser negativa")
    @Digits(integer = 11, fraction = 3, message = "A quantidade mínima aceita até 3 casas decimais")
    BigDecimal quantidadeMinima,

    Boolean ativo
) {
}
