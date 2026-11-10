package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.item.ItemService;
import br.com.canteiroconectado.support.RelogioAjustavel;
import br.com.canteiroconectado.support.TransacaoFalsa;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MovimentacaoService")
class MovimentacaoServiceTest {

    private static final UUID CANTEIRO = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private MovimentacaoEstoqueRepository movimentacoes;

    @Mock
    private SaldoEstoqueRepository saldos;

    @Mock
    private CanteiroService canteiros;

    @Mock
    private ItemService itens;

    private MovimentacaoService servico;
    private SaldoEstoque saldo;

    @BeforeEach
    void montar() {
        servico = new MovimentacaoService(movimentacoes, saldos, canteiros, itens,
            new RelogioAjustavel(Instant.parse("2026-11-01T12:00:00Z")), new TransacaoFalsa());
        saldo = saldoCom("2");
        when(saldos.travar(CANTEIRO, ITEM)).thenReturn(Optional.of(saldo));
        when(movimentacoes.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static SaldoEstoque saldoCom(String quantidade) {
        SaldoEstoque novo = mock(SaldoEstoque.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(novo, "canteiroId", CANTEIRO);
        ReflectionTestUtils.setField(novo, "itemId", ITEM);
        ReflectionTestUtils.setField(novo, "quantidade", new BigDecimal(quantidade));
        return novo;
    }

    private static NovaMovimentacao nova(TipoMovimentacao tipo, MotivoMovimentacao motivo, String quantidade) {
        return new NovaMovimentacao(UUID.randomUUID(), CANTEIRO, ITEM, tipo, motivo, new BigDecimal(quantidade),
            null, null, null, null, USUARIO);
    }

    private static ErrorCode codigo(Runnable acao) {
        try {
            acao.run();
            return null;
        } catch (BusinessException excecao) {
            return excecao.codigo();
        }
    }

    @Nested
    @DisplayName("aplicar")
    class Aplicar {

        @Test
        @DisplayName("deve criar o saldo se ausente, travar, gravar a movimentação com saldo_apos e baixar o saldo")
        void deve_baixarSaldo_quando_saida() {
            MovimentacaoEstoque gravada = servico.aplicar(nova(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "1"));

            InOrder ordem = inOrder(saldos, movimentacoes);
            ordem.verify(saldos).criarSeAusente(CANTEIRO, ITEM);
            ordem.verify(saldos).travar(CANTEIRO, ITEM);
            ordem.verify(movimentacoes).saveAndFlush(any());
            assertThat(gravada.getSaldoApos()).isEqualByComparingTo("1");
            assertThat(gravada.getQuantidade()).isEqualByComparingTo("1");
            assertThat(gravada.getCriadoEm().toInstant()).isEqualTo(Instant.parse("2026-11-01T12:00:00Z"));
            assertThat(saldo.getQuantidade()).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("deve somar ao saldo quando é entrada")
        void deve_somarSaldo_quando_entrada() {
            MovimentacaoEstoque gravada = servico.aplicar(
                nova(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "50"));

            assertThat(gravada.getSaldoApos()).isEqualByComparingTo("52");
            assertThat(saldo.getQuantidade()).isEqualByComparingTo("52");
        }

        @Test
        @DisplayName("deve recusar com SALDO_INSUFICIENTE e o saldo atual sem gravar nada")
        void deve_recusar_quando_saldoInsuficiente() {
            assertThatThrownBy(() -> servico.aplicar(nova(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "3")))
                .isInstanceOf(BusinessException.class)
                .satisfies(erro -> {
                    BusinessException excecao = (BusinessException) erro;
                    assertThat(excecao.codigo()).isEqualTo(ErrorCode.SALDO_INSUFICIENTE);
                    assertThat(excecao.propriedades()).containsEntry("saldoAtual", new BigDecimal("2"));
                });
            verify(movimentacoes, never()).saveAndFlush(any());
            assertThat(saldo.getQuantidade()).isEqualByComparingTo("2");
        }

        @Test
        @DisplayName("deve permitir zerar o saldo")
        void deve_permitirZerar_quando_saidaDoSaldoTodo() {
            assertThat(servico.aplicar(nova(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2")).getSaldoApos())
                .isEqualByComparingTo("0");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "0.0001"})
        @DisplayName("deve recusar com QUANTIDADE_INVALIDA zero, negativa ou com mais de 3 casas")
        void deve_recusar_quando_quantidadeInvalida(String quantidade) {
            assertThat(codigo(() -> servico.aplicar(
                nova(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, quantidade))))
                .isEqualTo(ErrorCode.QUANTIDADE_INVALIDA);
            verify(saldos, never()).travar(any(), any());
        }

        @Test
        @DisplayName("deve propagar ITEM_INATIVO e CANTEIRO_INATIVO antes de travar o saldo")
        void deve_recusar_quando_itemOuCanteiroInativo() {
            when(itens.exigirAtivo(ITEM)).thenThrow(new BusinessException(ErrorCode.ITEM_INATIVO, "inativo"));
            assertThat(codigo(() -> servico.aplicar(nova(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "1"))))
                .isEqualTo(ErrorCode.ITEM_INATIVO);

            when(canteiros.exigirAtivo(CANTEIRO)).thenThrow(new BusinessException(ErrorCode.CANTEIRO_INATIVO, "inativo"));
            assertThat(codigo(() -> servico.aplicar(nova(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "1"))))
                .isEqualTo(ErrorCode.CANTEIRO_INATIVO);
            verify(saldos, never()).travar(any(), any());
        }
    }
}
