package br.com.canteiroconectado.auditoria;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento da trilha de auditoria. Só de inserção: a entidade é imutável no Hibernate e o banco
 * rejeita {@code UPDATE} e {@code DELETE} pela trigger {@code bloquear_alteracao()}.
 */
@Entity
@Immutable
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false)
    private AcaoAuditoria acao;

    @Column(name = "entidade", nullable = false)
    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalhes")
    private Map<String, Object> detalhes;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected LogAuditoria() {
    }

    /**
     * Cria um evento novo.
     *
     * @param usuarioId autor da ação; nulo quando a ação é do próprio sistema ou de alguém não
     *     autenticado
     * @param detalhes dados complementares, nunca senhas nem tokens
     */
    public LogAuditoria(UUID usuarioId, AcaoAuditoria acao, String entidade, UUID entidadeId,
            Map<String, Object> detalhes, String requestId, OffsetDateTime criadoEm) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes == null || detalhes.isEmpty() ? null : Map.copyOf(detalhes);
        this.requestId = requestId;
        this.criadoEm = criadoEm.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public Map<String, Object> getDetalhes() {
        return detalhes;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
