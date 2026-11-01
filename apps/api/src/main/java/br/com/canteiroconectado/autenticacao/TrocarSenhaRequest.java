package br.com.canteiroconectado.autenticacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pedido de troca da própria senha.
 *
 * @param senhaAtual senha usada no login (a temporária, no primeiro acesso)
 * @param novaSenha nova senha, conferida pela {@link PoliticaDeSenha}
 */
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
