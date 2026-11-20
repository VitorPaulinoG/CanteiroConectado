package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Detalhe e estorno de movimentações")
class EstornoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private MovimentacaoService movimentacoes;

    @Autowired
    private LogAuditoriaRepository logs;

    @Autowired
    private EntityManager entityManager;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Item furadeira;
    private Colaborador joao;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        furadeira = dados.ferramenta();
        joao = dados.colaborador(aurora.getId());
    }

    private UUID movimentar(TipoMovimentacao tipo, MotivoMovimentacao motivo, String quantidade) {
        return movimentacoes.aplicar(new NovaMovimentacao(UUID.randomUUID(), aurora.getId(), furadeira.getId(), tipo,
            motivo, new BigDecimal(quantidade), tipo == TipoMovimentacao.SAIDA ? joao.getId() : null,
            tipo == TipoMovimentacao.SAIDA ? "Fundação" : null, null, null, sebastiao.getId())).getId();
    }

    private ResultActions estornar(Usuario autor, UUID original, String id, String motivo) throws Exception {
        String corpo = "{\"id\":\"" + id + "\"" + (motivo == null ? "" : ",\"motivo\":\"" + motivo + "\"") + "}";
        return mockMvc.perform(post("/api/v1/movimentacoes/" + original + "/estorno")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("POST /api/v1/movimentacoes/{id}/estorno")
    class Estornar {

        @Test
        @DisplayName("deve estornar a saída com uma ENTRADA de motivo ESTORNO que devolve o saldo e aponta para a original")
        void deve_devolverSaldo_quando_estornaSaida() throws Exception {
            movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "2");
            UUID saida = movimentar(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "1");

            estornar(sebastiao, saida, UUID.randomUUID().toString(), " Lançada no item errado ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.motivo").value("ESTORNO"))
                .andExpect(jsonPath("$.quantidade").value(1))
                .andExpect(jsonPath("$.saldoApos").value(2))
                .andExpect(jsonPath("$.estornaId").value(saida.toString()))
                .andExpect(jsonPath("$.observacao").value("Lançada no item errado"))
                .andExpect(jsonPath("$.colaboradorNome").value(joao.getNome()));
            entityManager.flush();

            assertThat(dados.saldo(aurora.getId(), furadeira.getId())).isEqualByComparingTo("2");
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(saida)).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.MOVIMENTACAO_ESTORNADA);
        }

        @Test
        @DisplayName("deve estornar a entrada com uma SAIDA quando há saldo, e recusar quando ele já foi consumido")
        void deve_exigirSaldo_quando_estornaEntrada() throws Exception {
            UUID recebimento = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "3");
            estornar(sebastiao, recebimento, UUID.randomUUID().toString(), "Nota errada")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.saldoApos").value(0));

            UUID outro = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "2");
            movimentar(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2");
            estornar(sebastiao, outro, UUID.randomUUID().toString(), "Nota errada")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SALDO_INSUFICIENTE"));
        }

        @Test
        @DisplayName("deve recusar com 409 JA_ESTORNADA o segundo estorno e com 422 o estorno de estorno")
        void deve_recusar_quando_estornoDuploOuDeEstorno() throws Exception {
            UUID entrada = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "5");
            String resposta = estornar(sebastiao, entrada, UUID.randomUUID().toString(), "Duplicada")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
            UUID estorno = UUID.fromString(JsonPath.read(resposta, "$.id"));

            estornar(sebastiao, entrada, UUID.randomUUID().toString(), "De novo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("JA_ESTORNADA"));
            estornar(sebastiao, estorno, UUID.randomUUID().toString(), "Desfazer")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ESTORNO_NAO_ESTORNAVEL"));
        }

        @Test
        @DisplayName("deve devolver o estorno já gravado com 200 quando o mesmo pedido se repete")
        void deve_devolverGravado_quando_pedidoRepetido() throws Exception {
            UUID entrada = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "5");
            String id = UUID.randomUUID().toString();
            estornar(sebastiao, entrada, id, "Duplicada").andExpect(status().isCreated());

            estornar(sebastiao, entrada, id, "Duplicada")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
            entityManager.flush();
            assertThat(dados.saldo(aurora.getId(), furadeira.getId())).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("deve exigir o motivo e recusar o GESTOR e o ALMOXARIFE de outro canteiro")
        void deve_recusar_quando_semMotivoOuSemPermissao() throws Exception {
            UUID entrada = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "5");

            estornar(sebastiao, entrada, UUID.randomUUID().toString(), "  ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_DO_ESTORNO_OBRIGATORIO"));
            estornar(tokens.gestor(), entrada, UUID.randomUUID().toString(), "Errado")
                .andExpect(status().isForbidden());
            estornar(tokens.almoxarife(), entrada, UUID.randomUUID().toString(), "Errado")
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/movimentacoes/{id}")
    class Detalhe {

        @Test
        @DisplayName("deve mostrar o registro completo e o vínculo com o estorno para os dois perfis")
        void deve_mostrarVinculo_quando_movimentacaoEstornada() throws Exception {
            movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "2");
            UUID saida = movimentar(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "1");
            String estorno = UUID.randomUUID().toString();
            estornar(sebastiao, saida, estorno, "Engano").andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/movimentacoes/" + saida).header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.motivo").value("RETIRADA"))
                .andExpect(jsonPath("$.itemDescricao").value(furadeira.getDescricao()))
                .andExpect(jsonPath("$.quantidade").value(1))
                .andExpect(jsonPath("$.saldoApos").value(1))
                .andExpect(jsonPath("$.canteiroNome").value(aurora.getNome()))
                .andExpect(jsonPath("$.frenteObra").value("Fundação"))
                .andExpect(jsonPath("$.colaboradorNome").value(joao.getNome()))
                .andExpect(jsonPath("$.registradoPorNome").value(sebastiao.getNome()))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.estornadaPorId").value(estorno));
            mockMvc.perform(get("/api/v1/movimentacoes/" + estorno).header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(jsonPath("$.estornaId").value(saida.toString()));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE de outro canteiro e com 404 o id inexistente")
        void deve_recusar_quando_outroCanteiroOuInexistente() throws Exception {
            UUID entrada = movimentar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "1");

            mockMvc.perform(get("/api/v1/movimentacoes/" + entrada).header("Authorization", tokens.bearer(tokens.almoxarife())))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/movimentacoes/" + UUID.randomUUID()).header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(status().isNotFound());
        }
    }
}
