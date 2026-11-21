package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarCanteiroRequest(
    @NotBlank(message = "Informe o nome do canteiro")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    String nome,

    @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
    String endereco
) {
}
