package br.com.canteiroconectado.colaborador;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar um colaborador.
 *
 * @param canteiroId obrigatório para GESTOR; o ALMOXARIFE usa o próprio canteiro quando omitido
 */
public record ColaboradorRequest(
    @NotBlank(message = "Informe o nome do colaborador")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    String nome,

    UUID canteiroId
) {
}
