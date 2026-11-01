package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.config.AdminProperties;

@SpringBootTest
@Transactional
@DisplayName("AdminInitializer no Postgres")
class AdminInitializerIntegracaoTest {

    @Autowired
    private AdminInitializer inicializador;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private LogAuditoriaRepository logs;

    @Autowired
    private AdminProperties propriedades;

    @Autowired
    private PasswordEncoder encoderSenha;

    @Autowired
    private JdbcTemplate jdbc;

    @Nested
    @DisplayName("subida da API")
    class Subida {

        @Test
        @DisplayName("deve ter criado o administrador das variáveis na subida, com o evento ADMIN_CRIADO, e não criar outro")
        void deve_existirUmAdministrador_quando_apiSobe() {
            Integer antes = jdbc.queryForObject("SELECT count(*) FROM usuario WHERE administrador", Integer.class);

            inicializador.run(null);

            Usuario admin = usuarios.findByEmailIgnoreCase(propriedades.email()).orElseThrow();
            assertThat(admin.isAdministrador()).isTrue();
            assertThat(encoderSenha.matches(propriedades.senha(), admin.getSenhaHash())).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE administrador", Integer.class))
                .isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE lower(email) = lower(?)", Integer.class,
                propriedades.email())).isEqualTo(1);
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(admin.getId()))
                .extracting(log -> log.getAcao())
                .containsExactly(AcaoAuditoria.ADMIN_CRIADO);
        }

        @Test
        @DisplayName("deve impedir UPDATE e DELETE no log_auditoria")
        void deve_impedirAlteracao_quando_logGravado() {
            assertThatThrownBy(() -> jdbc.update("UPDATE log_auditoria SET acao = 'X'"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Registro imutável");
        }
    }
}
