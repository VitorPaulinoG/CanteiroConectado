package br.com.canteiroconectado.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SenhaValidator")
class SenhaValidatorTest {

    @Nested
    @DisplayName("problema")
    class Problema {

        @ParameterizedTest
        @ValueSource(strings = {"abc12", "abcdefgh", "12345678", ""})
        @DisplayName("deve recusar senha curta, sem número ou sem letra")
        void deve_recusar_quando_senhaFraca(String senha) {
            assertThat(SenhaValidator.problema(senha)).isPresent();
        }

        @Test
        @DisplayName("deve recusar senha nula")
        void deve_recusar_quando_senhaNula() {
            assertThat(SenhaValidator.problema(null)).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {"canteiro1", "Obra2026!", "a1b2c3d4"})
        @DisplayName("deve aceitar senha com 8 caracteres ou mais, letra e número")
        void deve_aceitar_quando_senhaAtendeRegras(String senha) {
            assertThat(SenhaValidator.problema(senha)).isEmpty();
        }
    }
}
