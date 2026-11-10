package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.support.Dados;

@SpringBootTest
@Import(Dados.class)
@DisplayName("Bloqueio de linha do saldo")
class BloqueioDoSaldoTest {

    @Autowired
    private SaldoEstoqueRepository saldos;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private Dados dados;

    @Nested
    @DisplayName("travar")
    class Travar {

        @Test
        @DisplayName("deve fazer a segunda transação esperar a primeira terminar")
        void deve_esperar_quando_outraTransacaoTravou() throws Exception {
            UUID[] ids = transacao.execute(status -> {
                UUID canteiro = dados.canteiro().getId();
                UUID item = dados.material().getId();
                saldos.criarSeAusente(canteiro, item);
                return new UUID[] {canteiro, item};
            });
            CountDownLatch travou = new CountDownLatch(1);
            CountDownLatch liberar = new CountDownLatch(1);

            CompletableFuture<Void> primeira = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(status -> {
                saldos.travar(ids[0], ids[1]).orElseThrow();
                travou.countDown();
                aguardar(liberar);
            }));
            assertThat(travou.await(5, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Void> segunda = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(
                status -> saldos.travar(ids[0], ids[1]).orElseThrow()));
            Thread.sleep(400);
            assertThat(segunda).isNotDone();

            liberar.countDown();
            primeira.get(5, TimeUnit.SECONDS);
            segunda.get(5, TimeUnit.SECONDS);
            assertThat(segunda).isDone();
        }

        private void aguardar(CountDownLatch trava) {
            try {
                trava.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrompido) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
