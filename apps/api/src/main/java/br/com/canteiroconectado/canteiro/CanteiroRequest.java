package br.com.canteiroconectado.canteiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um canteiro.
 *
 * @param nome nome único do canteiro, sem diferenciar maiúsculas
 * @param endereco local da obra, opcional
 */
public record CanteiroRequest(
    @NotBlank(message = "Informe o nome do canteiro")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    String nome,

    @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
    String endereco
) {
}
