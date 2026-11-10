package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.comum.ResultadoDaGravacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;

/**
 * Gravações que chegam juntas, com dados confirmados no Postgres (sem transação de teste).
 */
@SpringBootTest
@Import({Tokens.class, Dados.class})
@DisplayName("Gravações simultâneas com o mesmo id")
class GravacoesSimultaneasTest {

    @Autowired
    private MovimentacaoService servico;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private Dados dados;

    @Autowired
    private Tokens tokens;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void encerrar() {
        executor.shutdownNow();
    }

    private record Cenario(UUID canteiro, UUID item, UUID outroItem, UUID autor) {
    }

    private Cenario preparar() {
        return transacao.execute(status -> {
            UUID canteiro = dados.canteiro().getId();
            return new Cenario(canteiro, dados.material().getId(), dados.material().getId(),
                tokens.almoxarife(canteiro).getId());
        });
    }

    private NovaMovimentacao entrada(UUID id, Cenario cenario, UUID item) {
        return new NovaMovimentacao(id, cenario.canteiro(), item, TipoMovimentacao.ENTRADA,
            MotivoMovimentacao.RECEBIMENTO, new BigDecimal("10"), null, null, null, null, cenario.autor());
    }

    private List<Object> aoMesmoTempo(List<Callable<ResultadoDaGravacao<MovimentacaoEstoque>>> tarefas)
            throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ResultadoDaGravacao<MovimentacaoEstoque>>> futuros = new ArrayList<>();
        for (Callable<ResultadoDaGravacao<MovimentacaoEstoque>> tarefa : tarefas) {
            futuros.add(executor.submit(() -> {
                largada.await();
                return tarefa.call();
            }));
        }
        largada.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<ResultadoDaGravacao<MovimentacaoEstoque>> futuro : futuros) {
            try {
                resultados.add(futuro.get(10, TimeUnit.SECONDS));
            } catch (ExecutionException falha) {
                resultados.add(falha.getCause());
            }
        }
        return resultados;
    }

    @Nested
    @DisplayName("aplicarUmaVez")
    class AplicarUmaVez {

        @RepeatedTest(3)
        @DisplayName("deve gravar só uma vez e devolver o registro gravado à outra quando o conteúdo é igual")
        void deve_gravarUmaVez_quando_mesmoConteudoAoMesmoTempo() throws Exception {
            Cenario cenario = preparar();
            NovaMovimentacao pedido = entrada(UUID.randomUUID(), cenario, cenario.item());

            List<Object> resultados = aoMesmoTempo(List.of(() -> servico.aplicarUmaVez(pedido),
                () -> servico.aplicarUmaVez(pedido)));

            assertThat(resultados).allMatch(ResultadoDaGravacao.class::isInstance);
            assertThat(resultados).extracting(r -> ((ResultadoDaGravacao<?>) r).novo())
                .containsExactlyInAnyOrder(true, false);
            assertThat(dados.contarMovimentacoes(cenario.canteiro(), cenario.item())).isEqualTo(1);
            assertThat(dados.saldo(cenario.canteiro(), cenario.item())).isEqualByComparingTo("10");
        }

        @RepeatedTest(3)
        @DisplayName("deve gravar uma e recusar a outra com ID_JA_UTILIZADO quando o conteúdo difere")
        void deve_recusarUma_quando_conteudoDiferenteAoMesmoTempo() throws Exception {
            Cenario cenario = preparar();
            UUID id = UUID.randomUUID();

            List<Object> resultados = aoMesmoTempo(List.of(
                () -> servico.aplicarUmaVez(entrada(id, cenario, cenario.item())),
                () -> servico.aplicarUmaVez(entrada(id, cenario, cenario.outroItem()))));

            assertThat(resultados).filteredOn(ResultadoDaGravacao.class::isInstance).hasSize(1)
                .allMatch(r -> ((ResultadoDaGravacao<?>) r).novo());
            assertThat(resultados).filteredOn(BusinessException.class::isInstance).singleElement()
                .extracting(r -> ((BusinessException) r).codigo())
                .isEqualTo(ErrorCode.ID_JA_UTILIZADO);
            int gravadas = dados.contarMovimentacoes(cenario.canteiro(), cenario.item())
                + dados.contarMovimentacoes(cenario.canteiro(), cenario.outroItem());
            assertThat(gravadas).isEqualTo(1);
        }
    }
}
