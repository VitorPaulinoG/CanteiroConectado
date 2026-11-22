package br.com.canteiroconectado.dto.response;

public record CredencialInicialResponse(String email, String senhaTemporaria) {

    @Override
    public String toString() {
        return "CredencialInicialResponse[email=" + email + ", senhaTemporaria=***]";
    }
}
