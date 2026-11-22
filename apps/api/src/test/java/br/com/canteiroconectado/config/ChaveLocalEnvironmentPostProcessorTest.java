package br.com.canteiroconectado.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("ChaveLocalEnvironmentPostProcessor")
class ChaveLocalEnvironmentPostProcessorTest {

    private final ChaveLocalEnvironmentPostProcessor processador = new ChaveLocalEnvironmentPostProcessor();

    @Nested
    @DisplayName("postProcessEnvironment")
    class PostProcessEnvironment {

        @Test
        @DisplayName("deve sortear uma chave de 32 bytes no perfil local quando a variável está vazia")
        void deve_sortearChave_quando_localSemChave() {
            MockEnvironment ambiente = new MockEnvironment().withProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE, "");
            ambiente.setActiveProfiles("local");

            processador.postProcessEnvironment(ambiente, new SpringApplication());

            assertThat(Base64.getDecoder().decode(ambiente.getProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE)))
                .hasSize(32);
        }

        @Test
        @DisplayName("não deve sortear chave no perfil prod")
        void naoDeve_sortearChave_quando_prod() {
            MockEnvironment ambiente = new MockEnvironment().withProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE, "");
            ambiente.setActiveProfiles("prod");

            processador.postProcessEnvironment(ambiente, new SpringApplication());

            assertThat(ambiente.getProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE)).isEmpty();
        }

        @Test
        @DisplayName("deve manter a chave definida pelo ambiente")
        void deve_manterChave_quando_definida() {
            MockEnvironment ambiente = new MockEnvironment().withProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE, "minha");
            ambiente.setActiveProfiles("local");

            processador.postProcessEnvironment(ambiente, new SpringApplication());

            assertThat(ambiente.getProperty(ChaveLocalEnvironmentPostProcessor.PROPRIEDADE)).isEqualTo("minha");
        }
    }
}
