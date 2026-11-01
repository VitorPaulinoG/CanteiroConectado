package br.com.canteiroconectado.auditoria;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("GET /api/v1/auditoria")
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private EntityManager entityManager;

    private Usuario admin;
    private Usuario camila;
    private Instant inicio;

    @BeforeEach
    void preparar() {
        admin = tokens.admin();
        camila = tokens.gestor();
        inicio = Instant.now().minusSeconds(1);
    }

    @Nested
    @DisplayName("filtros")
    class Filtros {

        @Test
        @DisplayName("deve mostrar o login com falha com usuário, horário e identificador da requisição")
        void deve_mostrarLoginFalhou_quando_filtraPorAcao() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .header("X-Request-Id", "req-auditoria-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + camila.getEmail() + "\",\"senha\":\"senha-errada-1\"}"))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/auditoria").param("acao", "LOGIN_FALHOU").param("de", inicio.toString())
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].acao").value("LOGIN_FALHOU"))
                .andExpect(jsonPath("$.conteudo[0].usuarioNome").value(camila.getNome()))
                .andExpect(jsonPath("$.conteudo[0].requestId").value("req-auditoria-1"))
                .andExpect(jsonPath("$.conteudo[0].detalhes.motivo").value("SENHA_INCORRETA"))
                .andExpect(jsonPath("$.conteudo[0].criadoEm").isNotEmpty());
        }

        @Test
        @DisplayName("deve mostrar autor e alvo da redefinição de senha, sem senha nem token")
        void deve_mostrarAutorEAlvo_quando_senhaRedefinida() throws Exception {
            mockMvc.perform(post("/api/v1/usuarios/" + camila.getId() + "/redefinir-senha")
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/auditoria").param("acao", "SENHA_REDEFINIDA").param("de", inicio.toString())
                    .header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(jsonPath("$.conteudo[0].usuarioNome").value(admin.getNome()))
                .andExpect(jsonPath("$.conteudo[0].entidade").value("usuario"))
                .andExpect(jsonPath("$.conteudo[0].entidadeDescricao").value(camila.getNome()))
                .andExpect(jsonPath("$.conteudo[0].detalhes.email").value(camila.getEmail()))
                .andExpect(jsonPath("$.conteudo[0].detalhes.senha").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].detalhes.senhaTemporaria").doesNotExist());
        }

        @Test
        @DisplayName("deve filtrar pelo autor e mostrar quem consultou a credencial inicial")
        void deve_filtrarAutor_quando_credencialConsultada() throws Exception {
            Usuario novo = tokens.gestor();
            mockMvc.perform(post("/api/v1/usuarios/" + novo.getId() + "/redefinir-senha")
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/usuarios/" + novo.getId() + "/credencial-inicial")
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isOk());
            entityManager.flush();

            mockMvc.perform(get("/api/v1/auditoria").param("usuarioId", admin.getId().toString())
                    .param("de", inicio.toString()).header("Authorization", tokens.bearer(camila)))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[*].usuarioNome", everyItem(is(admin.getNome()))))
                .andExpect(jsonPath("$.conteudo[0].acao").value("CREDENCIAL_CONSULTADA"))
                .andExpect(jsonPath("$.conteudo[0].entidadeDescricao").value(novo.getNome()));
        }

        @Test
        @DisplayName("deve recusar período invertido")
        void deve_recusar_quando_periodoInvertido() throws Exception {
            mockMvc.perform(get("/api/v1/auditoria").param("de", "2026-11-02T00:00:00Z").param("ate", "2026-11-01T00:00:00Z")
                    .header("Authorization", tokens.bearer(camila)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("de"));
        }
    }

    @Nested
    @DisplayName("permissão")
    class Permissao {

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE e não oferecer escrita")
        void deve_recusar_quando_almoxarife() throws Exception {
            mockMvc.perform(get("/api/v1/auditoria").header("Authorization", tokens.bearer(tokens.almoxarife())))
                .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/auditoria").header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isMethodNotAllowed());
        }
    }
}
