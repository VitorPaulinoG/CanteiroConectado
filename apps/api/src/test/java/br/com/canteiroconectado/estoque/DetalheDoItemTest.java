package br.com.canteiroconectado.estoque;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Detalhe do item no canteiro")
class DetalheDoItemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Item cimento;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        cimento = dados.item("Cimento CP-II 50kg", Unidade.SACO, TipoItem.MATERIAL, "50");
    }

    private void movimentacao(String tipo, String quantidade, String saldoApos, Instant quando) {
        jdbc.update("""
            INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos,
                                              registrado_por, criado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), aurora.getId(), cimento.getId(), tipo,
            tipo.equals("ENTRADA") ? "RECEBIMENTO" : "AJUSTE", new BigDecimal(quantidade), new BigDecimal(saldoApos),
            sebastiao.getId(), Timestamp.from(quando));
    }

    @Nested
    @DisplayName("GET /api/v1/canteiros/{id}/estoque/{itemId}")
    class Detalhar {

        @Test
        @DisplayName("deve mostrar saldo, mínimo, tipo, unidade e as 10 últimas movimentações da mais recente")
        void deve_mostrarDezUltimas_quando_itemComHistorico() throws Exception {
            Instant inicio = Instant.parse("2026-10-01T10:00:00Z");
            for (int i = 0; i < 12; i++) {
                movimentacao("ENTRADA", "5", String.valueOf(5 * (i + 1)), inicio.plus(Duration.ofHours(i)));
            }
            jdbc.update("INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade) VALUES (?, ?, 60)",
                aurora.getId(), cimento.getId());

            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/" + cimento.getId())
                    .header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Cimento CP-II 50kg"))
                .andExpect(jsonPath("$.unidadeSimbolo").value("saco"))
                .andExpect(jsonPath("$.tipo").value("MATERIAL"))
                .andExpect(jsonPath("$.saldo").value(60))
                .andExpect(jsonPath("$.quantidadeMinima").value(50))
                .andExpect(jsonPath("$.status").value("NORMAL"))
                .andExpect(jsonPath("$.ultimasMovimentacoes", hasSize(10)))
                .andExpect(jsonPath("$.ultimasMovimentacoes[0].saldoApos").value(60))
                .andExpect(jsonPath("$.ultimasMovimentacoes[9].saldoApos").value(15))
                .andExpect(jsonPath("$.ultimasMovimentacoes[0].registradoPorNome").value(sebastiao.getNome()));
        }

        @Test
        @DisplayName("deve mostrar saldo 0 e status ZERADO para o item sem movimentação no canteiro")
        void deve_mostrarZerado_quando_semMovimentacao() throws Exception {
            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/" + cimento.getId())
                    .header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.status").value("ZERADO"))
                .andExpect(jsonPath("$.ultimasMovimentacoes", hasSize(0)));
        }

        @Test
        @DisplayName("deve continuar consultável quando o item foi desativado")
        void deve_consultar_quando_itemDesativado() throws Exception {
            cimento.setAtivo(false);
            movimentacao("ENTRADA", "3", "3", Instant.parse("2026-10-01T10:00:00Z"));

            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/" + cimento.getId())
                    .header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.ultimasMovimentacoes[*].quantidade", contains(3)));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE de outro canteiro e com 404 item inexistente")
        void deve_recusar_quando_outroCanteiroOuItemInexistente() throws Exception {
            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/" + cimento.getId())
                    .header("Authorization", tokens.bearer(tokens.almoxarife())))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/" + UUID.randomUUID())
                    .header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(status().isNotFound());
        }
    }
}
