package br.com.canteiroconectado.usuario;

/**
 * E-mail e senha temporária para o administrador entregar ao funcionário. A senha nunca aparece
 * em {@link #toString()}.
 */
public record CredencialInicialResponse(String email, String senhaTemporaria) {

    @Override
    public String toString() {
        return "CredencialInicialResponse[email=" + email + ", senhaTemporaria=***]";
    }
}
