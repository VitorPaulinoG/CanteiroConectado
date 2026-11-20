package br.com.canteiroconectado.requisicao;

import jakarta.validation.constraints.Size;

/**
 * Recusa de uma requisição.
 *
 * @param motivo obrigatório, com 5 caracteres ou mais
 */
public record RecusaRequest(
    @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres")
    String motivo
) {
}
