package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    String senha
) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", senha=***]";
    }
}
