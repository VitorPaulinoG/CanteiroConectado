package br.com.canteiroconectado.item;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar um item.
 *
 * @param codigo código interno, único sem diferenciar maiúsculas
 * @param quantidadeMinima zero ou mais; zero significa "sem alerta"; padrão zero
 */
public record ItemRequest(
    @NotBlank(message = "Informe o código")
    @Size(max = 30, message = "O código deve ter no máximo 30 caracteres")
    String codigo,

    @NotBlank(message = "Informe a descrição")
    @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres")
    String descricao,

    @NotNull(message = "Informe a unidade")
    Unidade unidade,

    @NotNull(message = "Informe o tipo")
    TipoItem tipo,

    @DecimalMin(value = "0", message = "A quantidade mínima não pode ser negativa")
    @Digits(integer = 11, fraction = 3, message = "A quantidade mínima aceita até 3 casas decimais")
    BigDecimal quantidadeMinima
) {
}
