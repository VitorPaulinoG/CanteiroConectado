package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
    @NotBlank(message = "Informe a senha atual")
    String senhaAtual,

    @NotBlank(message = "Informe a nova senha")
    @Size(max = 72, message = "A nova senha deve ter no máximo 72 caracteres")
    String novaSenha
) {

    @Override
    public String toString() {
        return "TrocarSenhaRequest[senhaAtual=***, novaSenha=***]";
    }
}
