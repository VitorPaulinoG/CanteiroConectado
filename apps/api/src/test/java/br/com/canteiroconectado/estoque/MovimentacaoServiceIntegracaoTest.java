package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("MovimentacaoService no Postgres")
class MovimentacaoServiceIntegracaoTest {

    @Autowired
    private MovimentacaoService servico;

    @Autowired
    private Dados dados;

    @Autowired
    private Tokens tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private NovaMovimentacao nova(Canteiro canteiro, Item item, Usuario autor, TipoMovimentacao tipo,
            MotivoMovimentacao motivo, String quantidade) {
        return new NovaMovimentacao(UUID.randomUUID(), canteiro.getId(), item.getId(), tipo, motivo,
            new BigDecimal(quantidade), null, null, null, null, autor.getId());
    }

    @Nested
    @DisplayName("aplicar")
    class Aplicar {

        @Test
        @DisplayName("deve manter o saldo igual às entradas menos as saídas e gravar saldo_apos em cada linha")
        void deve_manterSaldoCoerente_quando_variasMovimentacoes() {
            Canteiro canteiro = dados.canteiro();
            Item cimento = dados.material();
            Usuario autor = tokens.almoxarife(canteiro.getId());

            servico.aplicar(nova(canteiro, cimento, autor, TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "50"));
            servico.aplicar(nova(canteiro, cimento, autor, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "12.5"));
            servico.aplicar(nova(canteiro, cimento, autor, TipoMovimentacao.ENTRADA, MotivoMovimentacao.AJUSTE, "0.25"));
            assertThatThrownBy(() -> servico.aplicar(
                nova(canteiro, cimento, autor, TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, "100")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("37.75");

            entityManager.flush();
            BigDecimal calculado = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN quantidade ELSE -quantidade END), 0)
                FROM movimentacao_estoque WHERE canteiro_id = ? AND item_id = ?
                """, BigDecimal.class, canteiro.getId(), cimento.getId());
            assertThat(dados.saldo(canteiro.getId(), cimento.getId())).isEqualByComparingTo(calculado)
                .isEqualByComparingTo("37.75");
            assertThat(jdbc.queryForList("""
                SELECT saldo_apos FROM movimentacao_estoque WHERE canteiro_id = ? AND item_id = ? ORDER BY saldo_apos DESC
                """, BigDecimal.class, canteiro.getId(), cimento.getId()))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("50"), new BigDecimal("37.75"), new BigDecimal("37.5"));
        }

        @Test
        @DisplayName("deve criar a linha de saldo na primeira movimentação do item no canteiro")
        void deve_criarSaldo_quando_primeiraMovimentacao() {
            Canteiro canteiro = dados.canteiro();
            Item furadeira = dados.ferramenta();
            Usuario autor = tokens.almoxarife(canteiro.getId());
            assertThat(dados.saldo(canteiro.getId(), furadeira.getId())).isNull();

            servico.aplicar(nova(canteiro, furadeira, autor, TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, "2"));
            entityManager.flush();

            assertThat(dados.saldo(canteiro.getId(), furadeira.getId())).isEqualByComparingTo("2");
        }
    }
}
