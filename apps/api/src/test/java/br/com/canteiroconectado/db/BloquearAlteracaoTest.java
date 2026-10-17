package br.com.canteiroconectado.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Exige o Postgres do Docker Compose (ou do CI): o Flyway aplica o V1 e a função é usada de verdade. */
@SpringBootTest
@Transactional
@DisplayName("bloquear_alteracao()")
class BloquearAlteracaoTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void criarTabelaDeTeste() {
        jdbc.execute("CREATE TEMPORARY TABLE registro_imutavel_teste (id INT PRIMARY KEY, texto TEXT)");
        jdbc.execute("""
            CREATE TRIGGER impedir_alteracao_registro_imutavel_teste
                BEFORE UPDATE OR DELETE ON registro_imutavel_teste
                FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao()
            """);
        jdbc.update("INSERT INTO registro_imutavel_teste (id, texto) VALUES (1, 'original')");
    }

    @Nested
    @DisplayName("trigger anexada a uma tabela")
    class TriggerAnexada {

        @Test
        @DisplayName("deve permitir INSERT quando a tabela usa a função")
        void deve_permitirInsert_quando_tabelaUsaFuncao() {
            jdbc.update("INSERT INTO registro_imutavel_teste (id, texto) VALUES (2, 'novo')");

            assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_imutavel_teste", Integer.class))
                .isEqualTo(2);
        }

        @Test
        @DisplayName("deve rejeitar UPDATE quando a tabela usa a função")
        void deve_rejeitarUpdate_quando_tabelaUsaFuncao() {
            assertThatThrownBy(() -> jdbc.update("UPDATE registro_imutavel_teste SET texto = 'alterado' WHERE id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Registro imutável");
        }

        @Test
        @DisplayName("deve rejeitar DELETE quando a tabela usa a função")
        void deve_rejeitarDelete_quando_tabelaUsaFuncao() {
            assertThatThrownBy(() -> jdbc.update("DELETE FROM registro_imutavel_teste WHERE id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Registro imutável");
        }
    }
}
