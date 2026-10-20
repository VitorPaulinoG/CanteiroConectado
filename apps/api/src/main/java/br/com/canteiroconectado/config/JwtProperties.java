package br.com.canteiroconectado.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "canteiro.jwt")
public record JwtProperties(
    @NotBlank(message = "A variável de ambiente JWT_SEGREDO é obrigatória")
    String segredo,

    @DefaultValue("12")
    @Positive(message = "A variável de ambiente JWT_VALIDADE_HORAS deve ser maior que zero")
    int validadeHoras
) {

    static final int TAMANHO_MINIMO_SEGREDO = 32;

    @AssertTrue(message = "A variável de ambiente JWT_SEGREDO deve ter ao menos 32 caracteres")
    boolean hasTamanhoMinimo() {
        return segredo == null || segredo.isBlank() || segredo.length() >= TAMANHO_MINIMO_SEGREDO;
    }

    @Override
    public String toString() {
        return "JwtProperties[segredo=***, validadeHoras=" + validadeHoras + "]";
    }
}
