package br.com.canteiroconectado.colaborador;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alteração parcial de um colaborador: só os campos informados mudam. O canteiro não muda.
 */
public record ColaboradorAtualizacaoRequest(
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar em branco")
    String nome,

    Boolean ativo
) {
}
