package br.com.canteiroconectado.usuario;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um funcionário.
 *
 * @param canteiroId obrigatório para ALMOXARIFE; ignorado para GESTOR
 */
public record UsuarioRequest(
    @NotBlank(message = "Informe o nome")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    String nome,

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres")
    String email,

    @NotNull(message = "Informe o perfil")
    Perfil perfil,

    UUID canteiroId
) {
}
