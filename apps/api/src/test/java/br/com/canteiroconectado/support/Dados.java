package br.com.canteiroconectado.support;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.canteiro.CanteiroRepository;
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.colaborador.ColaboradorRepository;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.ItemRepository;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;

/**
 * Cria itens, colaboradores e saldos de teste com nomes únicos, para que dados gravados por um
 * teste não apareçam nas buscas de outro.
 */
@Component
public class Dados {

    private final ItemRepository itens;
    private final ColaboradorRepository colaboradores;
    private final CanteiroRepository canteiros;
    private final JdbcTemplate jdbc;

    public Dados(ItemRepository itens, ColaboradorRepository colaboradores, CanteiroRepository canteiros,
            JdbcTemplate jdbc) {
        this.itens = itens;
        this.colaboradores = colaboradores;
        this.canteiros = canteiros;
        this.jdbc = jdbc;
    }

    public static String unico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public Canteiro canteiro() {
        return canteiros.saveAndFlush(new Canteiro(unico("Canteiro"), null));
    }

    public Item item(TipoItem tipo, String minimo) {
        String codigo = unico("TST");
        return itens.saveAndFlush(new Item(codigo, "Item de teste " + codigo, Unidade.UN, tipo, new BigDecimal(minimo)));
    }

    public Item item(String descricao, Unidade unidade, TipoItem tipo, String minimo) {
        return itens.saveAndFlush(new Item(unico("TST"), descricao, unidade, tipo, new BigDecimal(minimo)));
    }

    public Item ferramenta() {
        return item(TipoItem.FERRAMENTA, "0");
    }

    public Item material() {
        return item(TipoItem.MATERIAL, "0");
    }

    public Colaborador colaborador(UUID canteiroId) {
        return colaboradores.saveAndFlush(new Colaborador(unico("Colaborador"), canteiroId));
    }

    public BigDecimal saldo(UUID canteiroId, UUID itemId) {
        return jdbc.query("SELECT quantidade FROM saldo_estoque WHERE canteiro_id = ? AND item_id = ?",
            resultado -> resultado.next() ? resultado.getBigDecimal(1) : null, canteiroId, itemId);
    }

    public int contarMovimentacoes(UUID canteiroId, UUID itemId) {
        return jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque WHERE canteiro_id = ? AND item_id = ?",
            Integer.class, canteiroId, itemId);
    }

    public void inserirMovimentacao(UUID canteiroId, UUID itemId, UUID registradoPor, String tipo, String quantidade,
            String saldoApos) {
        jdbc.update("""
            INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos, registrado_por)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), canteiroId, itemId, tipo, tipo.equals("ENTRADA") ? "RECEBIMENTO" : "RETIRADA",
            new BigDecimal(quantidade), new BigDecimal(saldoApos), registradoPor);
    }
}
