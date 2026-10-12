package br.com.canteiroconectado.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "canteiro.admin")
public record AdminProperties(
    @NotBlank(message = "A variável de ambiente ADMIN_NOME é obrigatória")
    String nome,

    @NotBlank(message = "A variável de ambiente ADMIN_EMAIL é obrigatória")
    @Email(message = "A variável de ambiente ADMIN_EMAIL deve ser um e-mail válido")
    String email,

    @NotBlank(message = "A variável de ambiente ADMIN_SENHA é obrigatória")
    String senha
) {

    static final int TAMANHO_MINIMO_SENHA = 8;

    @AssertTrue(message = "A variável de ambiente ADMIN_SENHA deve ter ao menos 8 caracteres")
    boolean isSenhaComTamanhoMinimo() {
        return senha == null || senha.isBlank() || senha.length() >= TAMANHO_MINIMO_SENHA;
    }

    @Override
    public String toString() {
        return "AdminProperties[nome=" + nome + ", email=" + email + ", senha=***]";
    }
}
