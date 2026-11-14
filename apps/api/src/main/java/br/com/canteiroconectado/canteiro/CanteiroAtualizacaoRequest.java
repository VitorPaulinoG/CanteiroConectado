package br.com.canteiroconectado.canteiro;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alteração parcial de um canteiro: só os campos informados mudam. {@code ativo = false}
 * desativa o canteiro.
 */
public record CanteiroAtualizacaoRequest(
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar em branco")
    String nome,

    @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
    String endereco,

    Boolean ativo
) {
}
