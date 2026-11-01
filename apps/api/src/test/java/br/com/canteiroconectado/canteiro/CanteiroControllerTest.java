package br.com.canteiroconectado.canteiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("CanteiroController")
class CanteiroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private LogAuditoriaRepository logs;

    private Usuario admin;

    @BeforeEach
    void criarAdmin() {
        admin = tokens.admin();
    }

    private ResultActions criar(Usuario autor, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/canteiros")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("POST /api/v1/canteiros")
    class Criar {

        @Test
        @DisplayName("deve criar o canteiro ativo e registrar CANTEIRO_CRIADO quando o autor é o administrador")
        void deve_criar_quando_administrador() throws Exception {
            String id = com.jayway.jsonpath.JsonPath.read(
                criar(admin, "{\"nome\":\" Residencial Aurora \",\"endereco\":\"Rua das Obras, 100\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value("Residencial Aurora"))
                    .andExpect(jsonPath("$.endereco").value("Rua das Obras, 100"))
                    .andExpect(jsonPath("$.ativo").value(true))
                    .andReturn().getResponse().getContentAsString(), "$.id");

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(UUID.fromString(id)))
                .extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.CANTEIRO_CRIADO);
        }

        @Test
        @DisplayName("deve recusar com 409 CANTEIRO_DUPLICADO quando o nome já existe com outras maiúsculas")
        void deve_recusar_quando_nomeDuplicado() throws Exception {
            tokens.canteiro("Residencial Aurora");

            criar(admin, "{\"nome\":\"residencial AURORA\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_DUPLICADO"))
                .andExpect(jsonPath("$.detail").value("Já existe um canteiro com este nome."))
                .andExpect(jsonPath("$.campo").value("nome"));
        }

        @Test
        @DisplayName("deve recusar com 400 quando o nome está em branco")
        void deve_recusar_quando_nomeEmBranco() throws Exception {
            criar(admin, "{\"nome\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }

        @Test
        @DisplayName("deve recusar com 403 quando o autor é GESTOR sem a marca de administrador")
        void deve_recusar_quando_gestorComum() throws Exception {
            criar(tokens.gestor(), "{\"nome\":\"Obra X\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        }

        @Test
        @DisplayName("deve recusar com 403 quando o autor é ALMOXARIFE")
        void deve_recusar_quando_almoxarife() throws Exception {
            criar(tokens.almoxarife(), "{\"nome\":\"Obra Y\"}")
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/canteiros/{id}")
    class Atualizar {

        @Test
        @DisplayName("deve desativar o canteiro e registrar CANTEIRO_EDITADO")
        void deve_desativar_quando_ativoFalso() throws Exception {
            Canteiro canteiro = tokens.canteiro();

            mockMvc.perform(patch("/api/v1/canteiros/" + canteiro.getId())
                    .header("Authorization", tokens.bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(canteiro.getId()))
                .extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.CANTEIRO_EDITADO);
        }

        @Test
        @DisplayName("deve recusar renomear para o nome de outro canteiro")
        void deve_recusar_quando_renomeiaParaNomeExistente() throws Exception {
            tokens.canteiro("Edifício Sol");
            Canteiro outro = tokens.canteiro("Edifício Lua");

            mockMvc.perform(patch("/api/v1/canteiros/" + outro.getId())
                    .header("Authorization", tokens.bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"EDIFÍCIO SOL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_DUPLICADO"));
        }

        @Test
        @DisplayName("deve recusar com 404 quando o canteiro não existe")
        void deve_recusar_quando_canteiroInexistente() throws Exception {
            mockMvc.perform(patch("/api/v1/canteiros/" + UUID.randomUUID())
                    .header("Authorization", tokens.bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ativo\":false}"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/canteiros")
    class Listar {

        @Test
        @DisplayName("deve mostrar ao ALMOXARIFE só o próprio canteiro")
        void deve_mostrarSoProprio_quando_almoxarife() throws Exception {
            Canteiro meu = tokens.canteiro();
            Canteiro outro = tokens.canteiro();

            mockMvc.perform(get("/api/v1/canteiros").header("Authorization", tokens.bearer(tokens.almoxarife(meu.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(meu.getId().toString()));
            mockMvc.perform(get("/api/v1/canteiros/" + outro.getId())
                    .header("Authorization", tokens.bearer(tokens.almoxarife(meu.getId()))))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve mostrar ao GESTOR todos os canteiros e filtrar pela situação")
        void deve_mostrarTodos_quando_gestor() throws Exception {
            Canteiro ativo = tokens.canteiro();
            Canteiro inativo = tokens.canteiro();
            inativo.setAtivo(false);

            mockMvc.perform(get("/api/v1/canteiros").param("ativo", "true").header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ativo.getId().toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(inativo.getId().toString()))));
        }

        @Test
        @DisplayName("deve exigir token")
        void deve_exigirToken_quando_semToken() throws Exception {
            mockMvc.perform(get("/api/v1/canteiros")).andExpect(status().isUnauthorized());
        }
    }
}
