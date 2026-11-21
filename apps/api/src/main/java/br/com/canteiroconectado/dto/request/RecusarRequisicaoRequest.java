package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.Size;

public record RecusarRequisicaoRequest(
    @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres")
    String motivo
) {
}
