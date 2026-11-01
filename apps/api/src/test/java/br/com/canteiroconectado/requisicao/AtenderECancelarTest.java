package br.com.canteiroconectado.requisicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
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
@DisplayName("Atender e cancelar requisições")
class AtenderECancelarTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private Requisicoes requisicoes;

    @Autowired
    private LogAuditoriaRepository logs;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Usuario camila;
    private Item cimento;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        camila = tokens.gestor();
        cimento = dados.material();
    }

    private ResultActions acao(Usuario usuario, UUID id, String acao) throws Exception {
        return mockMvc.perform(post("/api/v1/requisicoes/" + id + "/" + acao)
            .header("Authorization", tokens.bearer(usuario)));
    }

    private UUID aprovadaPeloSebastiao() throws Exception {
        UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());
        mockMvc.perform(post("/api/v1/requisicoes/" + id + "/aprovar")
                .header("Authorization", tokens.bearer(sebastiao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());
        return id;
    }

    @Nested
    @DisplayName("POST /api/v1/requisicoes/{id}/atender")
    class Atender {

        @Test
        @DisplayName("deve marcar a aprovada como ATENDIDA pelo ALMOXARIFE sem criar entrada de estoque")
        void deve_atender_quando_aprovada() throws Exception {
            UUID id = aprovadaPeloSebastiao();

            acao(sebastiao, id, "atender")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENDIDA"))
                .andExpect(jsonPath("$.historico[*].status", contains("PENDENTE", "APROVADA", "ATENDIDA")))
                .andExpect(jsonPath("$.acoes.atender").value(false));

            assertThat(jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque WHERE item_id = ?", Integer.class,
                cimento.getId())).isZero();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.REQUISICAO_APROVADA, AcaoAuditoria.REQUISICAO_ATENDIDA);
        }

        @ParameterizedTest
        @ValueSource(strings = {"PENDENTE", "RECUSADA", "ATENDIDA", "CANCELADA"})
        @DisplayName("deve recusar com 409 REQUISICAO_NAO_APROVADA fora do status aprovada")
        void deve_recusar_quando_naoAprovada(String situacao) throws Exception {
            UUID id = requisicoes.gravar(camila, aurora.getId(), situacao, Instant.now(), cimento.getId());

            acao(sebastiao, id, "atender")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_NAO_APROVADA"));
        }

        @Test
        @DisplayName("deve recusar com 403 o GESTOR e o ALMOXARIFE de outro canteiro")
        void deve_recusar_quando_naoAlmoxarifeDoCanteiro() throws Exception {
            UUID id = aprovadaPeloSebastiao();

            acao(camila, id, "atender").andExpect(status().isForbidden());
            acao(tokens.almoxarife(), id, "atender").andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/requisicoes/{id}/cancelar")
    class Cancelar {

        @Test
        @DisplayName("deve cancelar a pendente pelo autor e registrar REQUISICAO_CANCELADA")
        void deve_cancelar_quando_autorEPendente() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            acao(camila, id, "cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.acoes.cancelar").value(false));
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.REQUISICAO_CANCELADA);
        }

        @Test
        @DisplayName("deve orientar falar com quem aprovou quando o autor tenta cancelar a aprovada")
        void deve_orientar_quando_autorCancelaAprovada() throws Exception {
            UUID id = aprovadaPeloSebastiao();

            acao(camila, id, "cancelar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_NAO_PENDENTE"))
                .andExpect(jsonPath("$.detail").value("Esta requisição já foi aprovada por " + sebastiao.getNome()
                    + ". Para desistir, fale com quem aprovou."));
        }

        @Test
        @DisplayName("deve recusar com 403 quem não é o autor")
        void deve_recusar_quando_naoAutor() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            acao(sebastiao, id, "cancelar")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Só quem criou a requisição pode cancelá-la."));
        }
    }
}
