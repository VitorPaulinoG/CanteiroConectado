package br.com.canteiroconectado.requisicao;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * Aprovação de uma requisição.
 *
 * @param previsaoChegada opcional; hoje ou depois
 * @param observacao recado opcional para quem pediu
 */
public record AprovacaoRequest(
    LocalDate previsaoChegada,

    @Size(max = 200, message = "A observação deve ter no máximo 200 caracteres")
    String observacao
) {
}
