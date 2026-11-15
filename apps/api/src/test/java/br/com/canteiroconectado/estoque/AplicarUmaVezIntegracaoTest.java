package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.comum.ResultadoDaGravacao;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;

@SpringBootTest
@Import({Tokens.class, Dados.class})
@DisplayName("MovimentacaoService.aplicarUmaVez no Postgres")
class AplicarUmaVezIntegracaoTest {

    @Autowired
    private MovimentacaoService servico;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private Dados dados;

    @Autowired
    private Tokens tokens;

    @Nested
    @DisplayName("mesmo id enviado duas vezes")
    class MesmoId {

        @Test
        @DisplayName("deve gravar uma vez e devolver o registro gravado na repetição, sem mudar o saldo")
        void deve_gravarUmaVez_quando_repetido() {
            UUID[] ids = transacao.execute(status -> {
                UUID canteiro = dados.canteiro().getId();
                return new UUID[] {canteiro, dados.material().getId(), tokens.almoxarife(canteiro).getId()};
            });
            NovaMovimentacao entrada = new NovaMovimentacao(UUID.randomUUID(), ids[0], ids[1],
                TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, new BigDecimal("50"), null, null,
                "NF 123", null, ids[2]);

            ResultadoDaGravacao<MovimentacaoEstoque> primeira = servico.aplicarUmaVez(entrada);
            ResultadoDaGravacao<MovimentacaoEstoque> segunda = servico.aplicarUmaVez(entrada);

            assertThat(primeira.novo()).isTrue();
            assertThat(segunda.novo()).isFalse();
            assertThat(segunda.registro().getId()).isEqualTo(entrada.id());
            assertThat(segunda.registro().getSaldoApos()).isEqualByComparingTo("50");
            assertThat(dados.saldo(ids[0], ids[1])).isEqualByComparingTo("50");
            assertThat(dados.contarMovimentacoes(ids[0], ids[1])).isEqualTo(1);
        }
    }
}
