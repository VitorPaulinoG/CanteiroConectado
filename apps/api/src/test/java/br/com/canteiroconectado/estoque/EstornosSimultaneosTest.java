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
 * Dois estornos da mesma movimentação ao mesmo tempo, com ids diferentes e dados confirmados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({Tokens.class, Dados.class})
@DisplayName("Estornos simultâneos")
class EstornosSimultaneosTest {

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
    @DisplayName("POST /api/v1/movimentacoes/{id}/estorno")
    class Estornar {

        @RepeatedTest(3)
        @DisplayName("deve gravar um estorno só e responder JA_ESTORNADA ao outro")
        void deve_gravarUmSo_quando_doisEstornosJuntos() throws Exception {
            UUID canteiro = transacao.execute(status -> dados.canteiro().getId());
            UUID item = transacao.execute(status -> dados.material().getId());
            Usuario almoxarife = transacao.execute(status -> tokens.almoxarife(canteiro));
            UUID entrada = movimentacoes.aplicarUmaVez(new NovaMovimentacao(UUID.randomUUID(), canteiro, item,
                TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, new BigDecimal("5"), null, null, null, null,
                almoxarife.getId())).registro().getId();
            String token = tokens.bearer(almoxarife);
            CountDownLatch largada = new CountDownLatch(1);

            List<Future<MockHttpServletResponse>> futuros = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String corpo = "{\"id\":\"" + UUID.randomUUID() + "\",\"motivo\":\"Nota duplicada\"}";
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return mockMvc.perform(post("/api/v1/movimentacoes/" + entrada + "/estorno")
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

            assertThat(respostas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
            assertThat(respostas).filteredOn(resposta -> resposta.getStatus() == 409).singleElement()
                .satisfies(resposta -> assertThat(resposta.getContentAsString()).contains("JA_ESTORNADA"));
            assertThat(dados.saldo(canteiro, item)).isEqualByComparingTo("0");
            assertThat(dados.contarMovimentacoes(canteiro, item)).isEqualTo(2);
        }
    }
}
