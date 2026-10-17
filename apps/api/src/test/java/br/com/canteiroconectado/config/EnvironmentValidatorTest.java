package br.com.canteiroconectado.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("EnvironmentValidator")
class EnvironmentValidatorTest {

    private static final String SEGREDO_VALIDO = "0123456789abcdef0123456789abcdef";

    private final EnvironmentValidator validador = new EnvironmentValidator();

    private MockEnvironment ambienteCompleto() {
        return new MockEnvironment()
            .withProperty("canteiro.banco.url", "jdbc:postgresql://localhost:5432/canteiroconectado")
            .withProperty("canteiro.banco.usuario", "usuario")
            .withProperty("canteiro.banco.senha", "senha")
            .withProperty("canteiro.jwt.segredo", SEGREDO_VALIDO)
            .withProperty("canteiro.admin.nome", "Administrador")
            .withProperty("canteiro.admin.email", "admin@canteiro.test")
            .withProperty("canteiro.admin.senha", "senha-forte-123");
    }

    @Nested
    @DisplayName("postProcessEnvironment")
    class PostProcessEnvironment {

        @Test
        @DisplayName("deve aceitar a configuração quando todas as variáveis obrigatórias estão preenchidas")
        void deve_aceitar_quando_ambienteCompleto() {
            assertThatCode(() -> validador.postProcessEnvironment(ambienteCompleto(), new SpringApplication()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve falhar nomeando JWT_SEGREDO quando o segredo está ausente")
        void deve_falharNomeandoVariavel_quando_jwtSegredoAusente() {
            MockEnvironment ambiente = ambienteCompleto();
            ambiente.withProperty("canteiro.jwt.segredo", "");

            assertThatThrownBy(() -> validador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SEGREDO");
        }

        @Test
        @DisplayName("deve falhar quando o segredo do JWT tem menos de 32 caracteres")
        void deve_falhar_quando_jwtSegredoCurto() {
            MockEnvironment ambiente = ambienteCompleto();
            ambiente.withProperty("canteiro.jwt.segredo", "curto");

            assertThatThrownBy(() -> validador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SEGREDO");
        }

        @Test
        @DisplayName("deve listar todas as variáveis ausentes quando várias faltam")
        void deve_listarTodas_quando_variasAusentes() {
            // Como no application.yml real: cada variável ausente chega como texto vazio.
            MockEnvironment ambiente = new MockEnvironment()
                .withProperty("canteiro.banco.url", "")
                .withProperty("canteiro.banco.usuario", "")
                .withProperty("canteiro.banco.senha", "")
                .withProperty("canteiro.jwt.segredo", "")
                .withProperty("canteiro.admin.nome", "")
                .withProperty("canteiro.admin.email", "")
                .withProperty("canteiro.admin.senha", "");

            assertThatThrownBy(() -> validador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(erro -> assertThat(erro.getMessage())
                    .contains("BANCO_URL", "BANCO_USUARIO", "BANCO_SENHA", "JWT_SEGREDO",
                        "ADMIN_NOME", "ADMIN_EMAIL", "ADMIN_SENHA"));
        }

        @Test
        @DisplayName("deve falhar quando a URL do banco não é JDBC do PostgreSQL")
        void deve_falhar_quando_urlNaoEhJdbc() {
            MockEnvironment ambiente = ambienteCompleto();
            ambiente.withProperty("canteiro.banco.url", "postgresql://usuario:senha@host/banco");

            assertThatThrownBy(() -> validador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BANCO_URL");
        }
    }
}
