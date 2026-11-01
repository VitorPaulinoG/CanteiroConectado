package br.com.canteiroconectado.requisicao;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Passo da linha do tempo da requisição: cada mudança de status, com autor, data e observação.
 * Só de inserção, protegido pela trigger {@code bloquear_alteracao()}.
 */
@Entity
@Immutable
@Table(name = "requisicao_evento")
public class RequisicaoEvento {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "requisicao_id", nullable = false)
    private UUID requisicaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusRequisicao status;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected RequisicaoEvento() {
    }

    public RequisicaoEvento(UUID requisicaoId, StatusRequisicao status, UUID usuarioId, String observacao,
            OffsetDateTime criadoEm) {
        this.id = UUID.randomUUID();
        this.requisicaoId = requisicaoId;
        this.status = status;
        this.usuarioId = usuarioId;
        this.observacao = observacao;
        this.criadoEm = criadoEm.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequisicaoId() {
        return requisicaoId;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
