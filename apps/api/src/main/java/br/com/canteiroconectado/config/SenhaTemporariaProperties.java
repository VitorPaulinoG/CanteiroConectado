package br.com.canteiroconectado.config;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "canteiro.senha-temporaria")
public record SenhaTemporariaProperties(
    @NotBlank(message = "A variável de ambiente CHAVE_SENHA_TEMPORARIA é obrigatória")
    String chave
) {

    static final int TAMANHO_CHAVE_BYTES = 32;

    @AssertTrue(message = "A variável de ambiente CHAVE_SENHA_TEMPORARIA deve ser Base64 de 32 bytes (openssl rand -base64 32)")
    boolean isChaveValida() {
        if (chave == null || chave.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(chave.strip()).length == TAMANHO_CHAVE_BYTES;
        } catch (IllegalArgumentException invalida) {
            return false;
        }
    }

    public byte[] chaveEmBytes() {
        return Base64.getDecoder().decode(chave.strip());
    }

    @Override
    public String toString() {
        return "SenhaTemporariaProperties[chave=***]";
    }
}
