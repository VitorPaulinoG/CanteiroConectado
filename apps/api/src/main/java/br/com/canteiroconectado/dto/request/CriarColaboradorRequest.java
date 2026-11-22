package br.com.canteiroconectado.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarColaboradorRequest(
    @NotBlank(message = "Informe o nome do colaborador")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    String nome,

    UUID canteiroId
) {
}
