package br.com.canteiroconectado.requisicao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.canteiroconectado.requisicao.RequisicaoService.PedidoNormalizado;
import br.com.canteiroconectado.usuario.Perfil;

@DisplayName("RequisicaoService.mesmoConteudo")
class MesmoPedidoDeRequisicaoTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID CANTEIRO = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CIMENTO = UUID.randomUUID();
    private static final UUID AREIA = UUID.randomUUID();
    private static final LocalDate DATA = LocalDate.of(2026, 11, 10);

    private static Requisicao gravada() {
        Requisicao requisicao = new Requisicao(ID, 1, CANTEIRO, AUTOR, Perfil.GESTOR, "Laje", "Obs", DATA,
            OffsetDateTime.now());
        requisicao.adicionarItem(CIMENTO, new BigDecimal("40.000"));
        requisicao.adicionarItem(AREIA, new BigDecimal("2"));
        return requisicao;
    }

    private static ItemDaRequisicaoRequest item(UUID itemId, String quantidade) {
        return new ItemDaRequisicaoRequest(itemId, quantidade == null ? null : new BigDecimal(quantidade));
    }

    private static PedidoNormalizado pedido(UUID canteiro, UUID autor, LocalDate data, String frente, String observacao,
            List<ItemDaRequisicaoRequest> itens) {
        return new PedidoNormalizado(ID, canteiro, autor, data, frente, observacao, itens);
    }

    private static List<ItemDaRequisicaoRequest> itensIguais() {
        return List.of(item(AREIA, "2"), item(CIMENTO, "40"));
    }

    static Stream<Arguments> diferencas() {
        UUID outro = UUID.randomUUID();
        return Stream.of(
            Arguments.of("canteiro", pedido(outro, AUTOR, DATA, "Laje", "Obs", itensIguais())),
            Arguments.of("autor", pedido(CANTEIRO, outro, DATA, "Laje", "Obs", itensIguais())),
            Arguments.of("data", pedido(CANTEIRO, AUTOR, DATA.plusDays(1), "Laje", "Obs", itensIguais())),
            Arguments.of("frente", pedido(CANTEIRO, AUTOR, DATA, "Fundação", "Obs", itensIguais())),
            Arguments.of("observação", pedido(CANTEIRO, AUTOR, DATA, "Laje", null, itensIguais())),
            Arguments.of("quantidade de um item", pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs",
                List.of(item(AREIA, "3"), item(CIMENTO, "40")))),
            Arguments.of("item trocado", pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs",
                List.of(item(outro, "2"), item(CIMENTO, "40")))),
            Arguments.of("item a menos", pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs", List.of(item(CIMENTO, "40")))),
            Arguments.of("item repetido", pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs",
                List.of(item(CIMENTO, "40"), item(CIMENTO, "40")))),
            Arguments.of("quantidade ausente", pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs",
                List.of(item(AREIA, null), item(CIMENTO, "40")))));
    }

    @Test
    @DisplayName("deve considerar o mesmo pedido com os itens em outra ordem e a quantidade em outra escala")
    void deve_considerarMesmoPedido_quando_soMudaOrdemEEscala() {
        assertThat(RequisicaoService.mesmoConteudo(gravada(), pedido(CANTEIRO, AUTOR, DATA, "Laje", "Obs",
            itensIguais()))).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diferencas")
    @DisplayName("deve tratar como outro pedido quando muda")
    void deve_tratarComoOutroPedido_quando_campoMuda(String campo, PedidoNormalizado pedido) {
        assertThat(RequisicaoService.mesmoConteudo(gravada(), pedido)).isFalse();
    }
}
