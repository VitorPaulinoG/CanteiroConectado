package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import br.com.canteiroconectado.filter.RequestIdFilter;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({SegurancaTest.ControllerDeTeste.class, Tokens.class})
@DisplayName("SecurityConfig")
class SegurancaTest {

    @RestController
    static class ControllerDeTeste {

        @PreAuthorize(Permissoes.ADMIN)
        @GetMapping("/api/v1/teste-seguranca/admin")
        String soAdmin() {
            return "ok";
        }

        @PreAuthorize(Permissoes.ALMOXARIFE)
        @GetMapping("/api/v1/teste-seguranca/almoxarife")
        String soAlmoxarife() {
            return "ok";
        }

        @GetMapping("/api/v1/teste-seguranca/eu")
        UsuarioLogado eu() {
            return UsuarioLogado.atual();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    private ListAppender<ILoggingEvent> acessos;

    @BeforeEach
    void capturarLogAcesso() {
        acessos = new ListAppender<>();
        acessos.start();
        ((Logger) LoggerFactory.getLogger("acesso")).addAppender(acessos);
    }

    @AfterEach
    void soltarLogAcesso() {
        ((Logger) LoggerFactory.getLogger("acesso")).detachAppender(acessos);
    }

    @Nested
    @DisplayName("autenticação")
    class Autenticacao {

        @Test
        @DisplayName("deve devolver 401 NAO_AUTENTICADO em Problem Details quando não há token")
        void deve_devolver401ProblemDetails_quando_semToken() throws Exception {
            mockMvc.perform(get("/api/v1/teste-seguranca/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("deve devolver 401 NAO_AUTENTICADO quando o token é inválido")
        void deve_devolver401_quando_tokenInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/teste-seguranca/eu").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        }

        @Test
        @DisplayName("deve manter públicos o ping e o health")
        void deve_manterPublicos_quando_pingEHealth() throws Exception {
            mockMvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("autorização por papel")
    class Autorizacao {

        @Test
        @DisplayName("deve devolver 403 ACESSO_NEGADO quando o papel não basta")
        void deve_devolver403_quando_papelInsuficiente() throws Exception {
            Usuario gestor = tokens.gestor();

            mockMvc.perform(get("/api/v1/teste-seguranca/admin").header("Authorization", tokens.bearer(gestor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mockMvc.perform(get("/api/v1/teste-seguranca/almoxarife").header("Authorization", tokens.bearer(gestor)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve liberar quando o papel confere")
        void deve_liberar_quando_papelConfere() throws Exception {
            mockMvc.perform(get("/api/v1/teste-seguranca/admin").header("Authorization", tokens.bearer(tokens.admin())))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/teste-seguranca/almoxarife")
                    .header("Authorization", tokens.bearer(tokens.almoxarife(UUID.randomUUID()))))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("UsuarioLogado e MDC")
    class UsuarioDaRequisicao {

        @Test
        @DisplayName("deve ler id, perfil, administrador e canteiro do token")
        void deve_lerClaims_quando_tokenValido() throws Exception {
            UUID canteiro = UUID.randomUUID();
            Usuario almoxarife = tokens.almoxarife(canteiro);

            mockMvc.perform(get("/api/v1/teste-seguranca/eu").header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(almoxarife.getId().toString()))
                .andExpect(jsonPath("$.perfil").value("ALMOXARIFE"))
                .andExpect(jsonPath("$.administrador").value(false))
                .andExpect(jsonPath("$.canteiroId").value(canteiro.toString()));
        }

        @Test
        @DisplayName("deve colocar o usuarioId no MDC da linha de acesso")
        void deve_colocarUsuarioNoMdc_quando_autenticado() throws Exception {
            Usuario gestor = tokens.gestor();

            mockMvc.perform(get("/api/v1/teste-seguranca/eu").header("Authorization", tokens.bearer(gestor)))
                .andExpect(status().isOk());

            assertThat(acessos.list).isNotEmpty();
            assertThat(acessos.list.getLast().getMDCPropertyMap())
                .containsEntry(RequestIdFilter.MDC_USUARIO, gestor.getId().toString());
        }
    }
}
