package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.canteiroconectado.config.SenhaTemporariaProperties;

@DisplayName("CifradorSenhaTemporaria")
class CifradorSenhaTemporariaTest {

    private static final String CHAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OUTRA_CHAVE = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());

    private final CifradorSenhaTemporaria cifrador = new CifradorSenhaTemporaria(new SenhaTemporariaProperties(CHAVE));
    private final UUID usuario = UUID.randomUUID();

    @Nested
    @DisplayName("cifrar e decifrar")
    class CifrarEDecifrar {

        @Test
        @DisplayName("deve devolver a senha original e nunca o texto em claro")
        void deve_devolverOriginal_quando_decifra() {
            String cifrada = cifrador.cifrar("Abc23defGH45", usuario);

            assertThat(cifrada).doesNotContain("Abc23defGH45");
            assertThat(cifrador.decifrar(cifrada, usuario)).isEqualTo("Abc23defGH45");
        }

        @Test
        @DisplayName("deve gerar textos diferentes para a mesma senha")
        void deve_gerarTextosDiferentes_quando_mesmaSenha() {
            assertThat(cifrador.cifrar("Abc23defGH45", usuario)).isNotEqualTo(cifrador.cifrar("Abc23defGH45", usuario));
        }

        @Test
        @DisplayName("deve recusar texto de outro usuário, alterado ou de outra chave")
        void deve_recusar_quando_textoNaoConfere() {
            String cifrada = cifrador.cifrar("Abc23defGH45", usuario);
            byte[] alterada = Base64.getDecoder().decode(cifrada);
            alterada[alterada.length - 1] ^= 1;
            CifradorSenhaTemporaria outro = new CifradorSenhaTemporaria(new SenhaTemporariaProperties(OUTRA_CHAVE));

            assertThatThrownBy(() -> cifrador.decifrar(cifrada, UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> cifrador.decifrar(Base64.getEncoder().encodeToString(alterada), usuario))
                .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> outro.decifrar(cifrada, usuario)).isInstanceOf(IllegalStateException.class);
        }
    }
}
