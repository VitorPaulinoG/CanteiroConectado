package br.com.canteiroconectado.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais de login.
 *
 * @param email e-mail do usuário, comparado sem diferenciar maiúsculas
 * @param senha senha em texto puro, nunca registrada em log
 */
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
