package br.com.canteiroconectado.requisicao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class, Requisicoes.class})
@DisplayName("Consulta de requisições")
class ConsultaDeRequisicoesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private Requisicoes requisicoes;

    private Canteiro aurora;
    private Canteiro outro;
    private Usuario sebastiao;
    private Usuario camila;
    private Usuario outraGestora;
    private Item cimento;
    private final Instant agora = Instant.now();

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        outro = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        camila = tokens.gestor();
        outraGestora = tokens.gestor();
        cimento = dados.material();
    }

    private UUID gravar(Usuario autor, Canteiro canteiro, String status, long minutosAtras) {
        return requisicoes.gravar(autor, canteiro.getId(), status, agora.minus(Duration.ofMinutes(minutosAtras)),
            cimento.getId());
    }

    private ResultActions pedir(Usuario usuario, MockHttpServletRequestBuilder pedido) throws Exception {
        return mockMvc.perform(pedido.header("Authorization", tokens.bearer(usuario)));
    }

    @Nested
    @DisplayName("GET /api/v1/requisicoes?escopo=minhas")
    class Minhas {

        @Test
        @DisplayName("deve mostrar só as do usuário no canteiro, pendentes primeiro e depois as mais recentes")
        void deve_ordenarPendentesPrimeiro_quando_minhas() throws Exception {
            UUID aprovadaRecente = gravar(camila, aurora, "APROVADA", 1);
            UUID pendenteAntiga = gravar(camila, aurora, "PENDENTE", 30);
            UUID pendenteNova = gravar(camila, aurora, "PENDENTE", 5);
            UUID recusadaAntiga = gravar(camila, aurora, "RECUSADA", 60);
            gravar(outraGestora, aurora, "PENDENTE", 2);
            gravar(camila, outro, "PENDENTE", 3);

            pedir(camila, get("/api/v1/requisicoes").param("canteiroId", aurora.getId().toString()).param("escopo", "minhas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.conteudo[*].id", contains(pendenteNova.toString(), pendenteAntiga.toString(),
                    aprovadaRecente.toString(), recusadaAntiga.toString())))
                .andExpect(jsonPath("$.conteudo[0].itens[0].quantidade").value(10))
                .andExpect(jsonPath("$.conteudo[0].numero").isNotEmpty());
        }

        @Test
        @DisplayName("deve filtrar pelo status e usar o canteiro do ALMOXARIFE quando não informado")
        void deve_filtrarStatus_quando_informado() throws Exception {
            gravar(sebastiao, aurora, "PENDENTE", 1);
            UUID aprovada = gravar(sebastiao, aurora, "APROVADA", 2);

            pedir(sebastiao, get("/api/v1/requisicoes").param("status", "APROVADA"))
                .andExpect(jsonPath("$.conteudo[*].id", contains(aprovada.toString())));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/requisicoes?escopo=para-decidir")
    class ParaDecidir {

        @Test
        @DisplayName("deve mostrar ao ALMOXARIFE só as pendentes de GESTOR no próprio canteiro")
        void deve_mostrarPendentesDoGestor_quando_almoxarife() throws Exception {
            UUID daCamila = gravar(camila, aurora, "PENDENTE", 5);
            UUID daOutraGestora = gravar(outraGestora, aurora, "PENDENTE", 1);
            gravar(camila, aurora, "APROVADA", 2);
            gravar(sebastiao, aurora, "PENDENTE", 3);
            gravar(camila, outro, "PENDENTE", 4);

            pedir(sebastiao, get("/api/v1/requisicoes").param("escopo", "para-decidir"))
                .andExpect(jsonPath("$.conteudo[*].id", containsInAnyOrder(daCamila.toString(), daOutraGestora.toString())))
                .andExpect(jsonPath("$.totalElementos").value(2));
        }

        @Test
        @DisplayName("deve mostrar ao GESTOR só as pendentes de ALMOXARIFE, e não as de outro GESTOR")
        void deve_mostrarPendentesDoAlmoxarife_quando_gestor() throws Exception {
            UUID doSebastiao = gravar(sebastiao, aurora, "PENDENTE", 5);
            gravar(outraGestora, aurora, "PENDENTE", 1);
            gravar(camila, aurora, "PENDENTE", 2);

            pedir(camila, get("/api/v1/requisicoes").param("canteiroId", aurora.getId().toString())
                    .param("escopo", "para-decidir"))
                .andExpect(jsonPath("$.conteudo[*].id", contains(doSebastiao.toString())));
        }

        @Test
        @DisplayName("deve recusar escopo desconhecido e canteiro de outro para o ALMOXARIFE")
        void deve_recusar_quando_escopoOuCanteiroInvalidos() throws Exception {
            pedir(sebastiao, get("/api/v1/requisicoes").param("escopo", "todas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("escopo"));
            pedir(sebastiao, get("/api/v1/requisicoes").param("canteiroId", outro.getId().toString()))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/requisicoes/{id}")
    class Detalhe {

        @Test
        @DisplayName("deve mostrar itens, linha do tempo e as ações de cada pessoa")
        void deve_mostrarAcoes_quando_abreDetalhe() throws Exception {
            UUID daCamila = gravar(camila, aurora, "PENDENTE", 5);

            pedir(sebastiao, get("/api/v1/requisicoes/" + daCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitanteNome").value(camila.getNome()))
                .andExpect(jsonPath("$.canteiroNome").value(aurora.getNome()))
                .andExpect(jsonPath("$.itens[0].codigo").value(cimento.getCodigo()))
                .andExpect(jsonPath("$.historico[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.historico[0].usuarioNome").value(camila.getNome()))
                .andExpect(jsonPath("$.acoes.decidir").value(true))
                .andExpect(jsonPath("$.acoes.cancelar").value(false));
            pedir(camila, get("/api/v1/requisicoes/" + daCamila))
                .andExpect(jsonPath("$.acoes.decidir").value(false))
                .andExpect(jsonPath("$.acoes.cancelar").value(true));
            pedir(outraGestora, get("/api/v1/requisicoes/" + daCamila))
                .andExpect(jsonPath("$.acoes.decidir").value(false))
                .andExpect(jsonPath("$.acoes.cancelar").value(false));
        }

        @Test
        @DisplayName("deve oferecer Atender ao ALMOXARIFE quando a requisição está aprovada")
        void deve_oferecerAtender_quando_aprovada() throws Exception {
            UUID aprovada = gravar(camila, aurora, "APROVADA", 5);

            pedir(sebastiao, get("/api/v1/requisicoes/" + aprovada)).andExpect(jsonPath("$.acoes.atender").value(true));
            pedir(camila, get("/api/v1/requisicoes/" + aprovada)).andExpect(jsonPath("$.acoes.atender").value(false));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE de outro canteiro e com 404 o id inexistente")
        void deve_recusar_quando_outroCanteiroOuInexistente() throws Exception {
            UUID deOutroCanteiro = gravar(camila, outro, "PENDENTE", 5);

            pedir(sebastiao, get("/api/v1/requisicoes/" + deOutroCanteiro)).andExpect(status().isForbidden());
            pedir(camila, get("/api/v1/requisicoes/" + UUID.randomUUID())).andExpect(status().isNotFound());
        }
    }
}
