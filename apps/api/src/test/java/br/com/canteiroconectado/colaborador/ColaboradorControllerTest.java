package br.com.canteiroconectado.colaborador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("ColaboradorController")
class ColaboradorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private ColaboradorRepository colaboradores;

    @Autowired
    private ColaboradorService servico;

    @Autowired
    private LogAuditoriaRepository logs;

    private Canteiro aurora;
    private Canteiro outro;
    private Usuario almoxarife;

    @BeforeEach
    void preparar() {
        aurora = tokens.canteiro();
        outro = tokens.canteiro();
        almoxarife = tokens.almoxarife(aurora.getId());
    }

    private ResultActions criar(Usuario autor, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/colaboradores")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("POST /api/v1/colaboradores")
    class Criar {

        @Test
        @DisplayName("deve cadastrar no canteiro do ALMOXARIFE sem informar o canteiro e registrar COLABORADOR_CRIADO")
        void deve_cadastrarNoProprioCanteiro_quando_almoxarife() throws Exception {
            criar(almoxarife, "{\"nome\":\" João da Silva \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("João da Silva"))
                .andExpect(jsonPath("$.canteiroId").value(aurora.getId().toString()))
                .andExpect(jsonPath("$.ativo").value(true));

            Colaborador criado = colaboradores.findAll().stream()
                .filter(c -> c.getCanteiroId().equals(aurora.getId())).findFirst().orElseThrow();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(criado.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.COLABORADOR_CRIADO);
        }

        @Test
        @DisplayName("deve recusar com 409 COLABORADOR_DUPLICADO o mesmo nome no mesmo canteiro")
        void deve_recusar_quando_nomeDuplicadoNoCanteiro() throws Exception {
            colaboradores.saveAndFlush(new Colaborador("João da Silva", aurora.getId()));

            criar(almoxarife, "{\"nome\":\"JOÃO DA SILVA\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("COLABORADOR_DUPLICADO"))
                .andExpect(jsonPath("$.campo").value("nome"));
        }

        @Test
        @DisplayName("deve aceitar o mesmo nome em outro canteiro")
        void deve_aceitar_quando_mesmoNomeEmOutroCanteiro() throws Exception {
            colaboradores.saveAndFlush(new Colaborador("João da Silva", outro.getId()));

            criar(almoxarife, "{\"nome\":\"João da Silva\"}").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE cadastrando em outro canteiro")
        void deve_recusar_quando_almoxarifeEmOutroCanteiro() throws Exception {
            criar(almoxarife, "{\"nome\":\"Maria\",\"canteiroId\":\"" + outro.getId() + "\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        }

        @Test
        @DisplayName("deve exigir que o GESTOR escolha o canteiro")
        void deve_exigirCanteiro_quando_gestor() throws Exception {
            Usuario gestor = tokens.gestor();

            criar(gestor, "{\"nome\":\"Maria\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
            criar(gestor, "{\"nome\":\"Maria\",\"canteiroId\":\"" + outro.getId() + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canteiroId").value(outro.getId().toString()));
        }

        @Test
        @DisplayName("deve recusar com 422 CANTEIRO_INATIVO")
        void deve_recusar_quando_canteiroInativo() throws Exception {
            aurora.setAtivo(false);

            criar(almoxarife, "{\"nome\":\"Maria\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_INATIVO"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/colaboradores")
    class Listar {

        @Test
        @DisplayName("deve buscar por nome sem diferenciar maiúsculas, com quem começa pelo termo primeiro")
        void deve_ordenarPorPrefixo_quando_busca() throws Exception {
            colaboradores.saveAndFlush(new Colaborador("Ana Joana", aurora.getId()));
            colaboradores.saveAndFlush(new Colaborador("Joana Prado", aurora.getId()));
            colaboradores.saveAndFlush(new Colaborador("Carlos", aurora.getId()));
            colaboradores.saveAndFlush(new Colaborador("Joana de Outra Obra", outro.getId()));

            mockMvc.perform(get("/api/v1/colaboradores").param("q", "JOANA")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Joana Prado", "Ana Joana")))
                .andExpect(jsonPath("$.totalElementos").value(2));
        }

        @Test
        @DisplayName("deve filtrar ativos e tratar % como texto")
        void deve_filtrarAtivos_quando_ativoTrue() throws Exception {
            Colaborador inativo = new Colaborador("Pedro 100%", aurora.getId());
            inativo.setAtivo(false);
            colaboradores.saveAndFlush(inativo);
            colaboradores.saveAndFlush(new Colaborador("Paulo", aurora.getId()));

            mockMvc.perform(get("/api/v1/colaboradores").param("ativo", "true")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Paulo")));
            mockMvc.perform(get("/api/v1/colaboradores").param("q", "%")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Pedro 100%")));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE listando outro canteiro")
        void deve_recusar_quando_almoxarifeListaOutroCanteiro() throws Exception {
            mockMvc.perform(get("/api/v1/colaboradores").param("canteiroId", outro.getId().toString())
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/colaboradores/{id}")
    class Atualizar {

        @Test
        @DisplayName("deve desativar o colaborador e registrar COLABORADOR_EDITADO")
        void deve_desativar_quando_ativoFalso() throws Exception {
            Colaborador joao = colaboradores.saveAndFlush(new Colaborador("João", aurora.getId()));

            mockMvc.perform(patch("/api/v1/colaboradores/" + joao.getId())
                    .header("Authorization", tokens.bearer(almoxarife))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(joao.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.COLABORADOR_EDITADO);
        }

        @Test
        @DisplayName("deve recusar com 403 o colaborador de outro canteiro")
        void deve_recusar_quando_colaboradorDeOutroCanteiro() throws Exception {
            Colaborador deOutro = colaboradores.saveAndFlush(new Colaborador("Maria", outro.getId()));

            mockMvc.perform(patch("/api/v1/colaboradores/" + deOutro.getId())
                    .header("Authorization", tokens.bearer(almoxarife))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Maria Souza\"}"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("exigirAtivoNoCanteiro")
    class ExigirAtivoNoCanteiro {

        @Test
        @DisplayName("deve recusar com COLABORADOR_INVALIDO quando é de outro canteiro, inativo ou inexistente")
        void deve_recusar_quando_colaboradorNaoServe() {
            Colaborador deOutro = colaboradores.saveAndFlush(new Colaborador("Maria", outro.getId()));
            Colaborador inativo = new Colaborador("Pedro", aurora.getId());
            inativo.setAtivo(false);
            colaboradores.saveAndFlush(inativo);

            for (UUID id : new UUID[] {deOutro.getId(), inativo.getId(), UUID.randomUUID()}) {
                assertThat(codigo(() -> servico.exigirAtivoNoCanteiro(id, aurora.getId())))
                    .isEqualTo(ErrorCode.COLABORADOR_INVALIDO);
            }
        }

        private ErrorCode codigo(Runnable acao) {
            try {
                acao.run();
                return null;
            } catch (BusinessException excecao) {
                return excecao.codigo();
            }
        }
    }
}
