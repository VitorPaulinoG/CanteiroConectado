package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarColaboradorRequest(
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar em branco")
    String nome,

    Boolean ativo
) {
}
