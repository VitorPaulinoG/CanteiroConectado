package br.com.canteiroconectado.requisicao;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Item pedido numa requisição, com a quantidade e a posição em que foi informado.
 */
@Entity
@Table(name = "item_requisicao")
public class ItemRequisicao {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false, updatable = false)
    private Requisicao requisicao;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "quantidade", nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "ordem", nullable = false, updatable = false)
    private int ordem;

    protected ItemRequisicao() {
    }

    ItemRequisicao(Requisicao requisicao, UUID itemId, BigDecimal quantidade, int ordem) {
        this.id = UUID.randomUUID();
        this.requisicao = requisicao;
        this.itemId = itemId;
        this.quantidade = quantidade;
        this.ordem = ordem;
    }

    public UUID getItemId() {
        return itemId;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public int getOrdem() {
        return ordem;
    }
}
