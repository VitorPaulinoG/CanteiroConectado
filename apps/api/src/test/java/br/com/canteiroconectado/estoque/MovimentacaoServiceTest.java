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
import java.time.OffsetDateTime;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    @Nested
    @DisplayName("aplicarUmaVez")
    class AplicarUmaVez {

        private final NovaMovimentacao saida = nova(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "1");

        private MovimentacaoEstoque gravada(NovaMovimentacao base, String quantidade) {
            return new MovimentacaoEstoque(base.id(), base.canteiroId(), base.itemId(), base.tipo(), base.motivo(),
                new BigDecimal(quantidade), new BigDecimal("1"), base.colaboradorId(), base.frenteObra(),
                base.observacao(), base.estornaId(), base.registradoPor(), OffsetDateTime.now());
        }

        @Test
        @DisplayName("deve gravar e marcar como novo quando o id ainda não existe")
        void deve_gravar_quando_idNovo() {
            when(movimentacoes.findById(saida.id())).thenReturn(Optional.empty());

            var resultado = servico.aplicarUmaVez(saida);

            assertThat(resultado.novo()).isTrue();
            assertThat(resultado.registro().getId()).isEqualTo(saida.id());
            verify(movimentacoes).saveAndFlush(any());
            assertThat(saldo.getQuantidade()).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("deve devolver o registro gravado sem tocar no saldo quando o mesmo pedido se repete")
        void deve_devolverGravado_quando_mesmoConteudo() {
            MovimentacaoEstoque existente = gravada(saida, "1.000");
            when(movimentacoes.findById(saida.id())).thenReturn(Optional.of(existente));

            var resultado = servico.aplicarUmaVez(saida);

            assertThat(resultado.novo()).isFalse();
            assertThat(resultado.registro()).isSameAs(existente);
            verify(saldos, never()).travar(any(), any());
            verify(movimentacoes, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deve recusar com ID_JA_UTILIZADO quando o id já tem outro conteúdo")
        void deve_recusar_quando_conteudoDivergente() {
            when(movimentacoes.findById(saida.id())).thenReturn(Optional.of(gravada(saida, "5")));

            assertThat(codigo(() -> servico.aplicarUmaVez(saida))).isEqualTo(ErrorCode.ID_JA_UTILIZADO);
            verify(movimentacoes, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deve devolver o registro da outra gravação quando perde a corrida pela chave primária")
        void deve_devolverOutraGravacao_quando_violacaoDeChave() {
            MovimentacaoEstoque daOutra = gravada(saida, "1");
            when(movimentacoes.findById(saida.id()))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(daOutra));
            when(movimentacoes.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("pk"));

            var resultado = servico.aplicarUmaVez(saida);

            assertThat(resultado.novo()).isFalse();
            assertThat(resultado.registro()).isSameAs(daOutra);
        }

        @Test
        @DisplayName("deve repassar a violação quando ela não é do id")
        void deve_repassar_quando_violacaoNaoEhDoId() {
            when(movimentacoes.findById(saida.id())).thenReturn(Optional.empty());
            when(movimentacoes.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("fk"));

            assertThatThrownBy(() -> servico.aplicarUmaVez(saida))
                .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("deve considerar o mesmo pedido só com os mesmos campos e autor")
        void deve_compararTodosOsCampos_quando_verificaConteudo() {
            MovimentacaoEstoque existente = gravada(saida, "1");
            NovaMovimentacao outroAutor = new NovaMovimentacao(saida.id(), CANTEIRO, ITEM, saida.tipo(),
                saida.motivo(), saida.quantidade(), null, null, null, null, UUID.randomUUID());
            NovaMovimentacao outraFrente = new NovaMovimentacao(saida.id(), CANTEIRO, ITEM, saida.tipo(),
                saida.motivo(), saida.quantidade(), null, "Fundação", null, null, USUARIO);

            assertThat(MovimentacaoService.mesmoConteudo(existente, saida)).isTrue();
            assertThat(MovimentacaoService.mesmoConteudo(existente, outroAutor)).isFalse();
            assertThat(MovimentacaoService.mesmoConteudo(existente, outraFrente)).isFalse();
        }
    }
}
