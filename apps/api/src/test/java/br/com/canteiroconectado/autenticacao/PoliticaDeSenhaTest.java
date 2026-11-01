package br.com.canteiroconectado.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PoliticaDeSenha")
class PoliticaDeSenhaTest {

    @Nested
    @DisplayName("problemaDa")
    class ProblemaDa {

        @ParameterizedTest
        @ValueSource(strings = {"abc12", "abcdefgh", "12345678", ""})
        @DisplayName("deve recusar senha curta, sem número ou sem letra")
        void deve_recusar_quando_senhaFraca(String senha) {
            assertThat(PoliticaDeSenha.problemaDa(senha)).isPresent();
        }

        @Test
        @DisplayName("deve recusar senha nula")
        void deve_recusar_quando_senhaNula() {
            assertThat(PoliticaDeSenha.problemaDa(null)).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {"canteiro1", "Obra2026!", "a1b2c3d4"})
        @DisplayName("deve aceitar senha com 8 caracteres ou mais, letra e número")
        void deve_aceitar_quando_senhaAtendeRegras(String senha) {
            assertThat(PoliticaDeSenha.problemaDa(senha)).isEmpty();
        }
    }
}
