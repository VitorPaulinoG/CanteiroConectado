package br.com.canteiroconectado.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "canteiro.banco")
public record DbProperties(
    @NotBlank(message = "A variável de ambiente BANCO_URL é obrigatória")
    String url,

    @NotBlank(message = "A variável de ambiente BANCO_USUARIO é obrigatória")
    String usuario,

    @NotBlank(message = "A variável de ambiente BANCO_SENHA é obrigatória")
    String senha
) {

    private static final String PREFIXO_URL = "jdbc:postgresql:";

    @AssertTrue(message = "A variável de ambiente BANCO_URL deve começar com jdbc:postgresql:")
    boolean isUrlPostgres() {
        return url == null || url.isBlank() || url.startsWith(PREFIXO_URL);
    }

    @Override
    public String toString() {
        return "DbProperties[url=***, usuario=" + usuario + ", senha=***]";
    }
}
