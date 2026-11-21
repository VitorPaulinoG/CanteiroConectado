package br.com.canteiroconectado.requisicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;

/**
 * Pedidos que chegam juntos, com os dados confirmados no Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({Tokens.class, Dados.class})
@DisplayName("Requisições simultâneas")
class RequisicoesSimultaneasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private JdbcTemplate jdbc;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void encerrar() {
        executor.shutdownNow();
    }

    private List<MockHttpServletResponse> aoMesmoTempo(String token, List<String> corpos) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> futuros = new ArrayList<>();
        for (String corpo : corpos) {
            futuros.add(executor.submit(() -> {
                largada.await();
                return mockMvc.perform(post("/api/v1/requisicoes")
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
        return respostas;
    }

    private String corpo(String id, UUID canteiro, UUID item) {
        return "{\"id\":\"" + id + "\",\"canteiroId\":\"" + canteiro + "\",\"necessarioAte\":\""
            + LocalDate.now(Fuso.DAS_OBRAS).plusDays(3) + "\",\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":5}]}";
    }

    @Nested
    @DisplayName("POST /api/v1/requisicoes")
    class Criar {

        @RepeatedTest(3)
        @DisplayName("deve dar números diferentes a dois pedidos diferentes no mesmo canteiro")
        void deve_numerarSemRepetir_quando_doisPedidosJuntos() throws Exception {
            UUID canteiro = transacao.execute(status -> dados.canteiro().getId());
            UUID item = transacao.execute(status -> dados.material().getId());
            String token = tokens.bearer(transacao.execute(status -> tokens.gestor()));

            List<MockHttpServletResponse> respostas = aoMesmoTempo(token, List.of(
                corpo(UUID.randomUUID().toString(), canteiro, item), corpo(UUID.randomUUID().toString(), canteiro, item)));

            assertThat(respostas).extracting(MockHttpServletResponse::getStatus).containsOnly(201);
            List<String> numeros = new ArrayList<>();
            for (MockHttpServletResponse resposta : respostas) {
                numeros.add(JsonPath.read(resposta.getContentAsString(), "$.numero"));
            }
            assertThat(numeros).containsExactlyInAnyOrder("REQ-0001", "REQ-0002");
        }

        @RepeatedTest(3)
        @DisplayName("deve criar uma só requisição quando o mesmo pedido chega duas vezes ao mesmo tempo")
        void deve_criarUmaVez_quando_mesmoPedidoJunto() throws Exception {
            UUID canteiro = transacao.execute(status -> dados.canteiro().getId());
            UUID item = transacao.execute(status -> dados.material().getId());
            String token = tokens.bearer(transacao.execute(status -> tokens.gestor()));
            String pedido = corpo(UUID.randomUUID().toString(), canteiro, item);

            List<MockHttpServletResponse> respostas = aoMesmoTempo(token, List.of(pedido, pedido));

            assertThat(respostas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(200, 201);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM requisicao WHERE canteiro_id = ?", Integer.class, canteiro))
                .isEqualTo(1);
        }
    }
}
