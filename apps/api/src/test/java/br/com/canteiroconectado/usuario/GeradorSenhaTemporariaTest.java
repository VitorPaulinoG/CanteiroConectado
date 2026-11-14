package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.canteiroconectado.autenticacao.PoliticaDeSenha;

@DisplayName("GeradorSenhaTemporaria")
class GeradorSenhaTemporariaTest {

    private final GeradorSenhaTemporaria gerador = new GeradorSenhaTemporaria();

    @Nested
    @DisplayName("gerar")
    class Gerar {

        @Test
        @DisplayName("deve gerar 12 caracteres que atendem à política de senha e não se repetem")
        void deve_gerarSenhaValida_quando_chamado() {
            Set<String> geradas = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                String senha = gerador.gerar();
                assertThat(senha).hasSize(12).doesNotContainAnyWhitespaces().matches("[A-Za-z2-9]+");
                assertThat(senha).doesNotContain("0", "O", "1", "l", "I");
                assertThat(PoliticaDeSenha.problemaDa(senha)).isEmpty();
                geradas.add(senha);
            }
            assertThat(geradas).hasSize(200);
        }
    }
}
