package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

/**
 * Duas saídas do último item ao mesmo tempo, pela API, com os dados confirmados no Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({Tokens.class, Dados.class})
@DisplayName("Saídas simultâneas do mesmo item")
class SaidasSimultaneasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimentacaoService movimentacoes;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void encerrar() {
        executor.shutdownNow();
    }

    @Nested
    @DisplayName("POST /api/v1/canteiros/{id}/movimentacoes")
    class Registrar {

        @RepeatedTest(3)
        @DisplayName("deve aceitar exatamente uma e recusar a outra com SALDO_INSUFICIENTE quando o saldo é 1")
        void deve_aceitarSoUma_quando_duasSaidasDoUltimoItem() throws Exception {
            UUID canteiro = transacao.execute(status -> dados.canteiro().getId());
            UUID item = transacao.execute(status -> dados.ferramenta().getId());
            Usuario almoxarife = transacao.execute(status -> tokens.almoxarife(canteiro));
            movimentacoes.aplicarUmaVez(new NovaMovimentacao(UUID.randomUUID(), canteiro, item,
                TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, BigDecimal.ONE, null, null, null, null,
                almoxarife.getId()));
            String token = tokens.bearer(almoxarife);
            CountDownLatch largada = new CountDownLatch(1);

            List<Future<MockHttpServletResponse>> futuros = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String corpo = "{\"id\":\"" + UUID.randomUUID() + "\",\"tipo\":\"SAIDA\",\"motivo\":\"RETIRADA\","
                    + "\"itemId\":\"" + item + "\",\"quantidade\":1}";
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return mockMvc.perform(post("/api/v1/canteiros/" + canteiro + "/movimentacoes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo)).andReturn().getResponse();
                }));
            }
            largada.countDown();
            List<MockHttpServletResponse> respostas = new ArrayList<>();
            for (Future<MockHttpServletResponse> futuro : futuros) {
                respostas.add(futuro.get(10, TimeUnit.SECONDS));
            }

            assertThat(respostas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 422);
            assertThat(respostas).filteredOn(resposta -> resposta.getStatus() == 422).singleElement()
                .satisfies(resposta -> assertThat(resposta.getContentAsString()).contains("SALDO_INSUFICIENTE"));
            assertThat(dados.saldo(canteiro, item)).isEqualByComparingTo("0");
            assertThat(dados.contarMovimentacoes(canteiro, item)).isEqualTo(2);
        }
    }
}
