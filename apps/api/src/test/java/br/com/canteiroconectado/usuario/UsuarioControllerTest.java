package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.support.Tokens;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("UsuarioController")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private LogAuditoriaRepository logs;

    private Usuario admin;
    private Canteiro aurora;

    @BeforeEach
    void preparar() {
        admin = tokens.admin();
        aurora = tokens.canteiro();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder pedido, Usuario autor, String corpo) throws Exception {
        pedido.header("Authorization", tokens.bearer(autor));
        if (corpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(pedido);
    }

    private String corpoAlmoxarife(String email, UUID canteiroId) {
        return "{\"nome\":\"Sebastião Lima\",\"email\":\"" + email + "\",\"perfil\":\"ALMOXARIFE\""
            + (canteiroId == null ? "" : ",\"canteiroId\":\"" + canteiroId + "\"") + "}";
    }

    private UUID criarAlmoxarife(String email) throws Exception {
        String resposta = enviar(post("/api/v1/usuarios"), admin, corpoAlmoxarife(email, aurora.getId()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    @Nested
    @DisplayName("POST /api/v1/usuarios")
    class Criar {

        @Test
        @DisplayName("deve criar ALMOXARIFE no canteiro com troca de senha obrigatória e registrar USUARIO_CRIADO")
        void deve_criarAlmoxarife_quando_dadosValidos() throws Exception {
            UUID id = criarAlmoxarife("Sebastiao@Obra.com");

            Usuario criado = usuarios.findById(id).orElseThrow();
            assertThat(criado.getEmail()).isEqualTo("sebastiao@obra.com");
            assertThat(criado.getCanteiroId()).isEqualTo(aurora.getId());
            assertThat(criado.isPrecisaTrocarSenha()).isTrue();
            assertThat(criado.isAdministrador()).isFalse();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.USUARIO_CRIADO);
        }

        @Test
        @DisplayName("deve criar GESTOR sem canteiro mesmo quando um canteiro é informado")
        void deve_criarGestorSemCanteiro_quando_canteiroInformado() throws Exception {
            enviar(post("/api/v1/usuarios"), admin,
                    "{\"nome\":\"Camila\",\"email\":\"camila@obra.com\",\"perfil\":\"GESTOR\",\"canteiroId\":\""
                        + aurora.getId() + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("GESTOR"))
                .andExpect(jsonPath("$.canteiroId").doesNotExist());
        }

        @Test
        @DisplayName("deve recusar com 409 EMAIL_JA_CADASTRADO quando o e-mail já existe com outras maiúsculas")
        void deve_recusar_quando_emailDuplicado() throws Exception {
            criarAlmoxarife("sebastiao@obra.com");

            enviar(post("/api/v1/usuarios"), admin, corpoAlmoxarife("SEBASTIAO@obra.com", aurora.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.campo").value("email"));
        }

        @Test
        @DisplayName("deve recusar com 422 CANTEIRO_OBRIGATORIO quando o ALMOXARIFE vem sem canteiro")
        void deve_recusar_quando_almoxarifeSemCanteiro() throws Exception {
            enviar(post("/api/v1/usuarios"), admin, corpoAlmoxarife("novo@obra.com", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_OBRIGATORIO"));
        }

        @Test
        @DisplayName("deve recusar com 422 CANTEIRO_INATIVO quando o canteiro está desativado")
        void deve_recusar_quando_canteiroInativo() throws Exception {
            aurora.setAtivo(false);

            enviar(post("/api/v1/usuarios"), admin, corpoAlmoxarife("novo@obra.com", aurora.getId()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_INATIVO"));
        }

        @Test
        @DisplayName("deve recusar com 403 quem não é administrador")
        void deve_recusar_quando_naoAdministrador() throws Exception {
            enviar(post("/api/v1/usuarios"), tokens.gestor(), corpoAlmoxarife("x@obra.com", aurora.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            enviar(get("/api/v1/usuarios"), tokens.almoxarife(), null)
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/usuarios")
    class Listar {

        @Test
        @DisplayName("deve filtrar por busca, perfil e situação com paginação")
        void deve_filtrar_quando_parametrosInformados() throws Exception {
            criarAlmoxarife("sebastiao@obra.com");
            UUID inativo = criarAlmoxarife("joao.inativo@obra.com");
            enviar(post("/api/v1/usuarios/" + inativo + "/desativar"), admin, null).andExpect(status().isOk());

            enviar(get("/api/v1/usuarios").param("q", "SEBAS").param("perfil", "ALMOXARIFE").param("ativo", "true"),
                    admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].email").value("sebastiao@obra.com"))
                .andExpect(jsonPath("$.conteudo[0].precisaTrocarSenha").value(true))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20));

            enviar(get("/api/v1/usuarios").param("ativo", "false"), admin, null)
                .andExpect(jsonPath("$.conteudo[*].email").value(hasItem("joao.inativo@obra.com")));
        }

        @Test
        @DisplayName("deve recusar tamanho de página acima de 100")
        void deve_recusar_quando_tamanhoAcimaDoMaximo() throws Exception {
            enviar(get("/api/v1/usuarios").param("tamanho", "101"), admin, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/usuarios/{id}")
    class Atualizar {

        @Test
        @DisplayName("deve trocar o ALMOXARIFE para GESTOR e retirar o canteiro")
        void deve_retirarCanteiro_quando_viraGestor() throws Exception {
            UUID id = criarAlmoxarife("sebastiao@obra.com");

            enviar(patch("/api/v1/usuarios/" + id), admin, "{\"perfil\":\"GESTOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("GESTOR"))
                .andExpect(jsonPath("$.canteiroId").doesNotExist());
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.USUARIO_CRIADO, AcaoAuditoria.USUARIO_EDITADO);
        }

        @Test
        @DisplayName("deve exigir canteiro quando o GESTOR vira ALMOXARIFE")
        void deve_exigirCanteiro_quando_viraAlmoxarife() throws Exception {
            Usuario gestor = tokens.gestor();

            enviar(patch("/api/v1/usuarios/" + gestor.getId()), admin, "{\"perfil\":\"ALMOXARIFE\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_OBRIGATORIO"));
        }

        @Test
        @DisplayName("deve proteger o perfil do administrador")
        void deve_protegerAdministrador_quando_mudaPerfil() throws Exception {
            enviar(patch("/api/v1/usuarios/" + admin.getId()), admin,
                    "{\"perfil\":\"ALMOXARIFE\",\"canteiroId\":\"" + aurora.getId() + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ADMINISTRADOR_PROTEGIDO"));
        }

        @Test
        @DisplayName("deve invalidar o token do funcionário quando o canteiro muda")
        void deve_invalidarToken_quando_canteiroMuda() throws Exception {
            Usuario almoxarife = tokens.almoxarife(aurora.getId());
            String tokenAntigo = tokens.bearer(almoxarife);
            Canteiro outro = tokens.canteiro();

            enviar(patch("/api/v1/usuarios/" + almoxarife.getId()), admin, "{\"canteiroId\":\"" + outro.getId() + "\"}")
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", tokenAntigo))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("redefinir, desativar e ativar")
    class Acoes {

        @Test
        @DisplayName("deve gerar nova senha, exigir troca, desbloquear e registrar SENHA_REDEFINIDA")
        void deve_redefinirSenha_quando_administrador() throws Exception {
            Usuario almoxarife = tokens.almoxarife(aurora.getId());
            almoxarife.setBloqueadoAte(OffsetDateTime.now().plusMinutes(10));
            String hashAntigo = almoxarife.getSenhaHash();

            enviar(post("/api/v1/usuarios/" + almoxarife.getId() + "/redefinir-senha"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precisaTrocarSenha").value(true));

            Usuario salvo = usuarios.findById(almoxarife.getId()).orElseThrow();
            assertThat(salvo.getSenhaHash()).isNotEqualTo(hashAntigo);
            assertThat(salvo.getBloqueadoAte()).isNull();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(almoxarife.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.SENHA_REDEFINIDA);
        }

        @Test
        @DisplayName("deve recusar com 422 AUTODESATIVACAO_PROIBIDA quando o administrador tenta se desativar")
        void deve_recusar_quando_autodesativacao() throws Exception {
            enviar(post("/api/v1/usuarios/" + admin.getId() + "/desativar"), admin, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("AUTODESATIVACAO_PROIBIDA"));
        }

        @Test
        @DisplayName("deve desativar e reativar o funcionário com auditoria")
        void deve_desativarEReativar_quando_administrador() throws Exception {
            Usuario gestor = tokens.gestor();

            enviar(post("/api/v1/usuarios/" + gestor.getId() + "/desativar"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
            enviar(post("/api/v1/usuarios/" + gestor.getId() + "/ativar"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(gestor.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.USUARIO_DESATIVADO, AcaoAuditoria.USUARIO_ATIVADO);
        }
    }
}
