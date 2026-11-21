package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Linha do livro de movimentações. Só de inserção: a entidade é imutável no Hibernate e o banco
 * rejeita {@code UPDATE} e {@code DELETE}. O {@code id} vem do app, para que um reenvio não
 * duplique o registro; a data oficial é a do servidor.
 */
@Entity
@Immutable
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "canteiro_id", nullable = false)
    private UUID canteiroId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimentacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false)
    private MotivoMovimentacao motivo;

    @Column(name = "quantidade", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "saldo_apos", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoApos;

    @Column(name = "colaborador_id")
    private UUID colaboradorId;

    @Column(name = "frente_obra")
    private String frenteObra;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "estorna_id")
    private UUID estornaId;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Transient
    private boolean nova = true;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(UUID id, UUID canteiroId, UUID itemId, TipoMovimentacao tipo,
            MotivoMovimentacao motivo, BigDecimal quantidade, BigDecimal saldoApos, UUID colaboradorId,
            String frenteObra, String observacao, UUID estornaId, UUID registradoPor, OffsetDateTime criadoEm) {
        this.id = id;
        this.canteiroId = canteiroId;
        this.itemId = itemId;
        this.tipo = tipo;
        this.motivo = motivo;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.colaboradorId = colaboradorId;
        this.frenteObra = frenteObra;
        this.observacao = observacao;
        this.estornaId = estornaId;
        this.registradoPor = registradoPor;
        this.criadoEm = criadoEm.withOffsetSameInstant(ZoneOffset.UTC);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nova;
    }

    @PostLoad
    @PostPersist
    void marcarGravada() {
        nova = false;
    }

    public UUID getCanteiroId() {
        return canteiroId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public MotivoMovimentacao getMotivo() {
        return motivo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getSaldoApos() {
        return saldoApos;
    }

    public UUID getColaboradorId() {
        return colaboradorId;
    }

    public String getFrenteObra() {
        return frenteObra;
    }

    public String getObservacao() {
        return observacao;
    }

    public UUID getEstornaId() {
        return estornaId;
    }

    public UUID getRegistradoPor() {
        return registradoPor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
