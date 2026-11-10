package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Invariante do saldo")
class InvarianteDoSaldoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimentacaoService movimentacoes;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Item furadeira;
    private Colaborador joao;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        furadeira = dados.ferramenta();
        joao = dados.colaborador(aurora.getId());
    }

    private UUID aplicar(TipoMovimentacao tipo, MotivoMovimentacao motivo, String quantidade, UUID estornaId) {
        boolean retirada = motivo == MotivoMovimentacao.RETIRADA;
        return movimentacoes.aplicar(new NovaMovimentacao(UUID.randomUUID(), aurora.getId(), furadeira.getId(), tipo,
            motivo, new BigDecimal(quantidade), retirada ? joao.getId() : null, retirada ? "Fundação" : null,
            motivo == MotivoMovimentacao.AJUSTE ? "Contagem" : null, estornaId, sebastiao.getId())).getId();
    }

    @Nested
    @DisplayName("livro de movimentações")
    class Livro {

        @Test
        @DisplayName("deve manter o saldo igual à soma do livro e cada saldo_apos igual ao acumulado, com todos os motivos")
        void deve_manterSaldoIgualAoLivro_quando_sequenciaComTodosOsMotivos() {
            aplicar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "10", null);
            UUID retirada = aplicar(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "3", null);
            aplicar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.DEVOLUCAO, "1", null);
            aplicar(TipoMovimentacao.SAIDA, MotivoMovimentacao.AJUSTE, "2", null);
            aplicar(TipoMovimentacao.ENTRADA, MotivoMovimentacao.ESTORNO, "3", retirada);
            aplicar(TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "9", null);
            entityManager.flush();

            List<BigDecimal[]> livro = jdbc.query("""
                SELECT saldo_apos,
                       SUM(CASE WHEN tipo = 'ENTRADA' THEN quantidade ELSE -quantidade END)
                           OVER (ORDER BY criado_em, id ROWS UNBOUNDED PRECEDING) AS acumulado
                FROM movimentacao_estoque WHERE canteiro_id = ? AND item_id = ?
                ORDER BY criado_em, id
                """, (linha, n) -> new BigDecimal[] {linha.getBigDecimal(1), linha.getBigDecimal(2)},
                aurora.getId(), furadeira.getId());

            assertThat(livro).hasSize(6).allSatisfy(par -> assertThat(par[0]).isEqualByComparingTo(par[1]));
            assertThat(livro.get(livro.size() - 1)[0]).isEqualByComparingTo("0");
            assertThat(dados.saldo(aurora.getId(), furadeira.getId())).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("estorno")
    class Estorno {

        @Test
        @DisplayName("deve devolver 404 ao estorno de uma movimentação que não existe")
        void deve_devolver404_quando_movimentacaoInexistente() throws Exception {
            mockMvc.perform(post("/api/v1/movimentacoes/" + UUID.randomUUID() + "/estorno")
                    .header("Authorization", tokens.bearer(sebastiao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":\"" + UUID.randomUUID() + "\",\"motivo\":\"Lançado errado\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
        }
    }
}
