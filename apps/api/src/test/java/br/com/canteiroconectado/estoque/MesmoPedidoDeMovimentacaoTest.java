package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("MovimentacaoService.mesmoConteudo")
class MesmoPedidoDeMovimentacaoTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID CANTEIRO = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();
    private static final UUID COLABORADOR = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID ORIGINAL = UUID.randomUUID();

    private static final NovaMovimentacao PEDIDO = new NovaMovimentacao(ID, CANTEIRO, ITEM, TipoMovimentacao.SAIDA,
        MotivoMovimentacao.RETIRADA, new BigDecimal("2"), COLABORADOR, "Fundação", "Obs", null, AUTOR);

    private static final MovimentacaoEstoque GRAVADA = new MovimentacaoEstoque(ID, CANTEIRO, ITEM,
        TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, new BigDecimal("2.000"), new BigDecimal("1"), COLABORADOR,
        "Fundação", "Obs", null, AUTOR, OffsetDateTime.now());

    private static NovaMovimentacao com(UUID canteiro, UUID item, TipoMovimentacao tipo, MotivoMovimentacao motivo,
            String quantidade, UUID colaborador, String frente, String observacao, UUID estornaId) {
        return new NovaMovimentacao(ID, canteiro, item, tipo, motivo, quantidade == null ? null : new BigDecimal(quantidade),
            colaborador, frente, observacao, estornaId, AUTOR);
    }

    static Stream<Arguments> diferencas() {
        UUID outro = UUID.randomUUID();
        return Stream.of(
            Arguments.of("canteiro", com(outro, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2",
                COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("item", com(CANTEIRO, outro, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2",
                COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("tipo", com(CANTEIRO, ITEM, TipoMovimentacao.ENTRADA, MotivoMovimentacao.RETIRADA, "2",
                COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("motivo", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.AJUSTE, "2",
                COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("quantidade", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "3",
                COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("quantidade ausente", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA,
                MotivoMovimentacao.RETIRADA, null, COLABORADOR, "Fundação", "Obs", null)),
            Arguments.of("colaborador", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2",
                outro, "Fundação", "Obs", null)),
            Arguments.of("observação", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2",
                COLABORADOR, "Fundação", "Outra", null)),
            Arguments.of("estorno", com(CANTEIRO, ITEM, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "2",
                COLABORADOR, "Fundação", "Obs", ORIGINAL)));
    }

    @Test
    @DisplayName("deve considerar o mesmo pedido com a quantidade em outra escala")
    void deve_considerarMesmoPedido_quando_soMudaAEscala() {
        assertThat(MovimentacaoService.mesmoConteudo(GRAVADA, PEDIDO)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diferencas")
    @DisplayName("deve tratar como outro pedido quando muda")
    void deve_tratarComoOutroPedido_quando_campoMuda(String campo, NovaMovimentacao pedido) {
        assertThat(MovimentacaoService.mesmoConteudo(GRAVADA, pedido)).isFalse();
    }
}
