package br.com.canteiroconectado.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AutenticacaoController")
class AutenticacaoControllerTest {

    private static final String SENHA_CORRETA = "senha-forte-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder encoderSenha;

    @Autowired
    private LogAuditoriaRepository logs;

    private Usuario gestor;

    @BeforeEach
    void criarUsuario() {
        gestor = usuarios.saveAndFlush(new Usuario("Gestora Teste", "gestora.login@canteiro.test",
            encoderSenha.encode(SENHA_CORRETA), Perfil.GESTOR, false, null));
    }

    private static String corpoLogin(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private String entrar(String email, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin(email, senha)))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    @Nested
    @DisplayName("POST /api/v1/autenticacao/login")
    class Login {

        @Test
        @DisplayName("deve devolver token e dados do usuário quando e-mail e senha estão corretos")
        void deve_devolverTokenEDados_quando_credenciaisCorretas() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin(gestor.getEmail(), SENHA_CORRETA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.nome").value("Gestora Teste"))
                .andExpect(jsonPath("$.usuario.perfil").value("GESTOR"))
                .andExpect(jsonPath("$.usuario.administrador").value(false))
                .andExpect(jsonPath("$.usuario.precisaTrocarSenha").value(false));
        }

        @Test
        @DisplayName("deve aceitar o e-mail com maiúsculas")
        void deve_aceitarEmail_quando_maiusculas() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin("GESTORA.Login@Canteiro.test", SENHA_CORRETA)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve devolver 401 CREDENCIAIS_INVALIDAS quando a senha está errada")
        void deve_devolver401_quando_senhaErrada() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin(gestor.getEmail(), "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        }

        @Test
        @DisplayName("deve devolver a mesma resposta quando o e-mail não existe")
        void deve_devolverMesmaResposta_quando_emailInexistente() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin("ninguem@canteiro.test", SENHA_CORRETA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        }

        @Test
        @DisplayName("deve devolver 403 USUARIO_INATIVO quando o usuário está desativado")
        void deve_devolver403_quando_usuarioDesativado() throws Exception {
            gestor.setAtivo(false);
            usuarios.saveAndFlush(gestor);

            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin(gestor.getEmail(), SENHA_CORRETA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("USUARIO_INATIVO"));
        }

        @Test
        @DisplayName("deve devolver 423 CONTA_BLOQUEADA com bloqueadoAte depois de cinco senhas erradas")
        void deve_devolver423_quando_cincoSenhasErradas() throws Exception {
            for (int tentativa = 1; tentativa <= 4; tentativa++) {
                mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(gestor.getEmail(), "senha-errada")))
                    .andExpect(status().isUnauthorized());
            }
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin(gestor.getEmail(), "senha-errada")))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.codigo").value("CONTA_BLOQUEADA"))
                .andExpect(jsonPath("$.bloqueadoAte").isNotEmpty());

            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin(gestor.getEmail(), SENHA_CORRETA)))
                .andExpect(status().isLocked());

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(gestor.getId()))
                .extracting(LogAuditoria::getAcao)
                .containsSubsequence(AcaoAuditoria.LOGIN_FALHOU, AcaoAuditoria.CONTA_BLOQUEADA,
                    AcaoAuditoria.LOGIN_FALHOU);
        }

        @Test
        @DisplayName("deve devolver 400 VALIDACAO_INVALIDA quando o corpo é inválido")
        void deve_devolver400_quando_corpoInvalido() throws Exception {
            mockMvc.perform(post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLogin("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/autenticacao/eu")
    class Eu {

        @Test
        @DisplayName("deve devolver os dados do usuário quando o token é válido")
        void deve_devolverDados_quando_tokenValido() throws Exception {
            String token = entrar(gestor.getEmail(), SENHA_CORRETA);

            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gestor.getId().toString()))
                .andExpect(jsonPath("$.email").value(gestor.getEmail()))
                .andExpect(jsonPath("$.perfil").value("GESTOR"))
                .andExpect(jsonPath("$.canteiroId").doesNotExist());
        }

        @Test
        @DisplayName("deve devolver 401 quando não há token")
        void deve_devolver401_quando_semToken() throws Exception {
            mockMvc.perform(get("/api/v1/autenticacao/eu"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve devolver 401 quando o token é inválido")
        void deve_devolver401_quando_tokenInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        }
    }
}
