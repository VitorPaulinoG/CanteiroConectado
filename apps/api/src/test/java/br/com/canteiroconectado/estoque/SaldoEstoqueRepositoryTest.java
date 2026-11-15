package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.UsoDoItem;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Livro de movimentações e saldo no Postgres")
class SaldoEstoqueRepositoryTest {

    @Autowired
    private SaldoEstoqueRepository saldos;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacoes;

    @Autowired
    private List<UsoDoItem> usos;

    @Autowired
    private Dados dados;

    @Autowired
    private Tokens tokens;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro canteiro;
    private Item item;
    private Usuario almoxarife;

    @BeforeEach
    void preparar() {
        canteiro = dados.canteiro();
        item = dados.ferramenta();
        almoxarife = tokens.almoxarife(canteiro.getId());
    }

    @Nested
    @DisplayName("SaldoEstoqueRepository")
    class Saldo {

        @Test
        @DisplayName("deve criar a linha de saldo zerada uma única vez e devolvê-la travada")
        void deve_criarUmaVez_quando_chamadoDuasVezes() {
            saldos.criarSeAusente(canteiro.getId(), item.getId());
            saldos.criarSeAusente(canteiro.getId(), item.getId());

            SaldoEstoque saldo = saldos.travar(canteiro.getId(), item.getId()).orElseThrow();
            assertThat(saldo.getQuantidade()).isEqualByComparingTo("0");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM saldo_estoque WHERE item_id = ?", Integer.class,
                item.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("deve recusar saldo negativo no banco")
        void deve_recusarSaldoNegativo_quando_atualizadoDireto() {
            saldos.criarSeAusente(canteiro.getId(), item.getId());

            assertThatThrownBy(() -> jdbc.update("UPDATE saldo_estoque SET quantidade = -1 WHERE item_id = ?", item.getId()))
                .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("deve subir a versão ao gravar o saldo")
        void deve_subirVersao_quando_saldoMuda() {
            saldos.criarSeAusente(canteiro.getId(), item.getId());
            SaldoEstoque saldo = saldos.travar(canteiro.getId(), item.getId()).orElseThrow();

            saldo.setQuantidade(new BigDecimal("3"));
            saldos.saveAndFlush(saldo);

            assertThat(saldo.getVersao()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("movimentacao_estoque")
    class Livro {

        @Test
        @DisplayName("deve rejeitar UPDATE de uma movimentação gravada")
        void deve_rejeitarUpdate_quando_movimentacaoGravada() {
            dados.inserirMovimentacao(canteiro.getId(), item.getId(), almoxarife.getId(), "ENTRADA", "2", "2");

            assertThatThrownBy(() -> jdbc.update("UPDATE movimentacao_estoque SET quantidade = 5 WHERE item_id = ?",
                item.getId())).isInstanceOf(DataAccessException.class).hasMessageContaining("Registro imutável");
        }

        @Test
        @DisplayName("deve rejeitar DELETE de uma movimentação gravada")
        void deve_rejeitarDelete_quando_movimentacaoGravada() {
            dados.inserirMovimentacao(canteiro.getId(), item.getId(), almoxarife.getId(), "ENTRADA", "2", "2");

            assertThatThrownBy(() -> jdbc.update("DELETE FROM movimentacao_estoque WHERE item_id = ?", item.getId()))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Registro imutável");
        }

        @Test
        @DisplayName("deve recusar quantidade zero")
        void deve_recusar_quando_quantidadeZero() {
            assertThatThrownBy(() -> dados.inserirMovimentacao(canteiro.getId(), item.getId(), almoxarife.getId(),
                "ENTRADA", "0", "0")).isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("deve recusar estorno sem a movimentação original")
        void deve_recusar_quando_estornoSemOriginal() {
            assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos, registrado_por)
                VALUES (gen_random_uuid(), ?, ?, 'ENTRADA', 'ESTORNO', 1, 1, ?)
                """, canteiro.getId(), item.getId(), almoxarife.getId())).isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("deve informar ao catálogo que o item já foi movimentado")
        void deve_informarUso_quando_itemTemMovimentacao() {
            assertThat(usos).anyMatch(uso -> !uso.jaMovimentado(item.getId()));

            dados.inserirMovimentacao(canteiro.getId(), item.getId(), almoxarife.getId(), "ENTRADA", "1", "1");

            assertThat(movimentacoes.existsByItemId(item.getId())).isTrue();
            assertThat(usos).anyMatch(uso -> uso.jaMovimentado(item.getId()));
        }
    }
}
