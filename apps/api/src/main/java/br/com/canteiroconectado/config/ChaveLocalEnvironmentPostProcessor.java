package br.com.canteiroconectado.config;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

/**
 * No perfil {@code local}, sorteia uma {@code CHAVE_SENHA_TEMPORARIA} quando ela não foi
 * definida, para a API subir sem configuração. Como a chave muda a cada subida, as senhas
 * temporárias cifradas antes deixam de poder ser lidas; basta redefinir a senha do funcionário.
 * Roda antes do {@link EnvironmentValidator}.
 */
public class ChaveLocalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPRIEDADE = "canteiro.senha-temporaria.chave";

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        String atual = ambiente.getProperty(PROPRIEDADE);
        if ((atual == null || atual.isBlank()) && ambiente.acceptsProfiles(Profiles.of("local"))) {
            byte[] chave = new byte[SenhaTemporariaProperties.TAMANHO_CHAVE_BYTES];
            new SecureRandom().nextBytes(chave);
            ambiente.getPropertySources().addFirst(new MapPropertySource("chaveLocalSorteada",
                Map.of(PROPRIEDADE, Base64.getEncoder().encodeToString(chave))));
        }
    }
}
