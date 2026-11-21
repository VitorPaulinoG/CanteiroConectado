package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.support.Tokens;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("Credencial inicial")
class CredencialInicialTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private LogAuditoriaRepository logs;

    private Usuario admin;
    private UUID funcionario;

    @BeforeEach
    void criarFuncionario() throws Exception {
        admin = tokens.admin();
        String resposta = mockMvc.perform(post("/api/v1/usuarios")
                .header("Authorization", tokens.bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Camila\",\"email\":\"camila.credencial@obra.com\",\"perfil\":\"GESTOR\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.credencialInicialDisponivel").value(true))
            .andReturn().getResponse().getContentAsString();
        funcionario = UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    private String consultarSenha() throws Exception {
        String resposta = mockMvc.perform(get("/api/v1/usuarios/" + funcionario + "/credencial-inicial")
                .header("Authorization", tokens.bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("camila.credencial@obra.com"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.senhaTemporaria");
    }

    private String entrar(String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"camila.credencial@obra.com\",\"senha\":\"" + senha + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.precisaTrocarSenha").value(true))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    @Nested
    @DisplayName("GET /api/v1/usuarios/{id}/credencial-inicial")
    class Consultar {

        @Test
        @DisplayName("deve devolver a senha temporária de 12 caracteres que entra no app, e registrar a consulta sem a senha")
        void deve_devolverSenhaQueEntra_quando_antesDaTroca() throws Exception {
            String senha = consultarSenha();

            assertThat(senha).hasSize(12);
            entrar(senha);
            assertThat(consultarSenha()).isEqualTo(senha);
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(funcionario))
                .filteredOn(log -> log.getAcao() == AcaoAuditoria.CREDENCIAL_CONSULTADA)
                .hasSize(2)
                .allSatisfy(log -> {
                    assertThat(log.getUsuarioId()).isEqualTo(admin.getId());
                    assertThat(String.valueOf(log.getDetalhes())).doesNotContain(senha);
                });
        }

        @Test
        @DisplayName("deve responder 409 CREDENCIAL_JA_TROCADA depois que o funcionário troca a senha")
        void deve_responder409_quando_senhaTrocada() throws Exception {
            String senha = consultarSenha();
            String token = entrar(senha);
            mockMvc.perform(post("/api/v1/autenticacao/trocar-senha")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"senhaAtual\":\"" + senha + "\",\"novaSenha\":\"minha-senha-7\"}"))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/usuarios/" + funcionario + "/credencial-inicial")
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAL_JA_TROCADA"));
            mockMvc.perform(get("/api/v1/usuarios/" + funcionario).header("Authorization", tokens.bearer(admin)))
                .andExpect(jsonPath("$.credencialInicialDisponivel").value(false));
        }

        @Test
        @DisplayName("deve gerar outra senha temporária consultável depois de redefinir")
        void deve_gerarOutraSenha_quando_redefine() throws Exception {
            String primeira = consultarSenha();

            mockMvc.perform(post("/api/v1/usuarios/" + funcionario + "/redefinir-senha")
                    .header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isOk());

            assertThat(consultarSenha()).isNotEqualTo(primeira).hasSize(12);
        }

        @Test
        @DisplayName("deve recusar com 403 quem não é administrador")
        void deve_recusar_quando_naoAdministrador() throws Exception {
            mockMvc.perform(get("/api/v1/usuarios/" + funcionario + "/credencial-inicial")
                    .header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isForbidden());
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(funcionario)).extracting(LogAuditoria::getAcao)
                .doesNotContain(AcaoAuditoria.CREDENCIAL_CONSULTADA);
        }
    }
}
