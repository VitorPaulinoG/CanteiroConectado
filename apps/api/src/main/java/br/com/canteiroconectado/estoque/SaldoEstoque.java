package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Saldo de um item num canteiro. Nasce na primeira movimentação e muda só na mesma transação que
 * grava a movimentação, com a linha bloqueada; o banco recusa saldo negativo.
 */
@Entity
@IdClass(SaldoEstoqueId.class)
@Table(name = "saldo_estoque")
public class SaldoEstoque {

    @Id
    @Column(name = "canteiro_id", nullable = false, updatable = false)
    private UUID canteiroId;

    @Id
    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "quantidade", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected SaldoEstoque() {
    }

    public UUID getCanteiroId() {
        return canteiroId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public long getVersao() {
        return versao;
    }
}
