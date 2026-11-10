package br.com.canteiroconectado.requisicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class, Requisicoes.class})
@DisplayName("Decisão de requisições")
class DecisaoDeRequisicaoTest {

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

    private ResultActions decidir(Usuario usuario, UUID id, String acao, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/requisicoes/" + id + "/" + acao)
            .header("Authorization", tokens.bearer(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("POST /api/v1/requisicoes/{id}/aprovar")
    class Aprovar {

        @Test
        @DisplayName("deve aprovar a requisição da GESTOR pelo ALMOXARIFE com a previsão de chegada e registrar o evento")
        void deve_aprovar_quando_almoxarifeDecideDoGestor() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());
            LocalDate previsao = LocalDate.now(Fuso.DAS_OBRAS).plusDays(5);

            decidir(sebastiao, id, "aprovar", "{\"previsaoChegada\":\"" + previsao + "\",\"observacao\":\"Pedido feito\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.previsaoChegada").value(previsao.toString()))
                .andExpect(jsonPath("$.decididoPorNome").value(sebastiao.getNome()))
                .andExpect(jsonPath("$.decididoEm").isNotEmpty())
                .andExpect(jsonPath("$.observacaoDecisao").value("Pedido feito"))
                .andExpect(jsonPath("$.historico[*].status", contains("PENDENTE", "APROVADA")))
                .andExpect(jsonPath("$.acoes.decidir").value(false))
                .andExpect(jsonPath("$.acoes.atender").value(true));

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.REQUISICAO_APROVADA);
        }

        @Test
        @DisplayName("deve aprovar sem corpo e sem previsão")
        void deve_aprovar_quando_semPrevisao() throws Exception {
            UUID id = requisicoes.pendente(sebastiao, aurora.getId(), cimento.getId());

            mockMvc.perform(post("/api/v1/requisicoes/" + id + "/aprovar").header("Authorization", tokens.bearer(camila)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.previsaoChegada").doesNotExist());
        }

        @Test
        @DisplayName("deve recusar com 403 AUTOAPROVACAO_PROIBIDA quando o autor tenta decidir")
        void deve_recusar_quando_autorDecide() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            decidir(camila, id, "aprovar", "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AUTOAPROVACAO_PROIBIDA"));
        }

        @Test
        @DisplayName("deve recusar com 403 o GESTOR decidindo a requisição de outro GESTOR")
        void deve_recusar_quando_mesmoPerfil() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            decidir(tokens.gestor(), id, "aprovar", "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.detail").value("As requisições do gestor são decididas pelo almoxarife."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"APROVADA", "RECUSADA", "ATENDIDA", "CANCELADA"})
        @DisplayName("deve recusar com 409 REQUISICAO_NAO_PENDENTE a requisição que não está pendente")
        void deve_recusar_quando_naoPendente(String situacao) throws Exception {
            UUID id = requisicoes.gravar(camila, aurora.getId(), situacao, Instant.now(), cimento.getId());

            decidir(sebastiao, id, "aprovar", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_NAO_PENDENTE"))
                .andExpect(jsonPath("$.status").value(situacao));
            decidir(sebastiao, id, "recusar", "{\"motivo\":\"Sem verba\"}")
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_NAO_PENDENTE"));
        }

        @Test
        @DisplayName("deve recusar a previsão de chegada anterior a hoje")
        void deve_recusar_quando_previsaoNoPassado() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            decidir(sebastiao, id, "aprovar",
                    "{\"previsaoChegada\":\"" + LocalDate.now(Fuso.DAS_OBRAS).minusDays(1) + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DATA_NO_PASSADO"))
                .andExpect(jsonPath("$.campo").value("previsaoChegada"));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE de outro canteiro")
        void deve_recusar_quando_almoxarifeDeOutroCanteiro() throws Exception {
            UUID id = requisicoes.pendente(camila, aurora.getId(), cimento.getId());

            decidir(tokens.almoxarife(), id, "aprovar", "{}").andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/requisicoes/{id}/recusar")
    class Recusar {

        @Test
        @DisplayName("deve recusar a requisição do ALMOXARIFE pela GESTOR com o motivo")
        void deve_recusar_quando_gestorDecideDoAlmoxarife() throws Exception {
            UUID id = requisicoes.pendente(sebastiao, aurora.getId(), cimento.getId());

            decidir(camila, id, "recusar", "{\"motivo\":\" Material já está no canteiro \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"))
                .andExpect(jsonPath("$.observacaoDecisao").value("Material já está no canteiro"))
                .andExpect(jsonPath("$.historico[1].observacao").value("Material já está no canteiro"));
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(id)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.REQUISICAO_RECUSADA);
        }

        @ParameterizedTest
        @ValueSource(strings = {"{}", "{\"motivo\":\"\"}", "{\"motivo\":\"  não \"}"})
        @DisplayName("deve exigir motivo com 5 caracteres ou mais")
        void deve_exigirMotivo_quando_ausenteOuCurto(String corpo) throws Exception {
            UUID id = requisicoes.pendente(sebastiao, aurora.getId(), cimento.getId());

            decidir(camila, id, "recusar", corpo)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_DA_RECUSA_OBRIGATORIO"));
        }
    }
}
