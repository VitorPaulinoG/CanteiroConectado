package br.com.canteiroconectado.requisicao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import br.com.canteiroconectado.usuario.Perfil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Pedido de um ou mais itens para um canteiro. O {@code id} vem do app, para que um reenvio não
 * duplique o pedido; o número legível (REQ-0042) é sequencial por canteiro. O perfil de quem pediu
 * é guardado na criação, porque é ele que define quem decide.
 */
@Entity
@Table(name = "requisicao")
public class Requisicao implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero", nullable = false, updatable = false)
    private int numero;

    @Column(name = "canteiro_id", nullable = false, updatable = false)
    private UUID canteiroId;

    @Column(name = "solicitante_id", nullable = false, updatable = false)
    private UUID solicitanteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "solicitante_perfil", nullable = false, updatable = false)
    private Perfil solicitantePerfil;

    @Column(name = "frente_obra", updatable = false)
    private String frenteObra;

    @Column(name = "observacao", updatable = false)
    private String observacao;

    @Column(name = "necessario_ate", nullable = false, updatable = false)
    private LocalDate necessarioAte;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusRequisicao status;

    @Column(name = "decidido_por")
    private UUID decididoPor;

    @Column(name = "decidido_em")
    private OffsetDateTime decididoEm;

    @Column(name = "observacao_decisao")
    private String observacaoDecisao;

    @Column(name = "previsao_chegada")
    private LocalDate previsaoChegada;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<ItemRequisicao> itens = new ArrayList<>();

    @Transient
    private boolean nova = true;

    protected Requisicao() {
    }

    public Requisicao(UUID id, int numero, UUID canteiroId, UUID solicitanteId, Perfil solicitantePerfil,
            String frenteObra, String observacao, LocalDate necessarioAte, OffsetDateTime criadoEm) {
        this.id = id;
        this.numero = numero;
        this.canteiroId = canteiroId;
        this.solicitanteId = solicitanteId;
        this.solicitantePerfil = solicitantePerfil;
        this.frenteObra = frenteObra;
        this.observacao = observacao;
        this.necessarioAte = necessarioAte;
        this.status = StatusRequisicao.PENDENTE;
        this.criadoEm = criadoEm.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public void adicionarItem(UUID itemId, BigDecimal quantidade) {
        itens.add(new ItemRequisicao(this, itemId, quantidade, itens.size()));
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

    public int getNumero() {
        return numero;
    }

    public UUID getCanteiroId() {
        return canteiroId;
    }

    public UUID getSolicitanteId() {
        return solicitanteId;
    }

    public Perfil getSolicitantePerfil() {
        return solicitantePerfil;
    }

    public String getFrenteObra() {
        return frenteObra;
    }

    public String getObservacao() {
        return observacao;
    }

    public LocalDate getNecessarioAte() {
        return necessarioAte;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public void setStatus(StatusRequisicao status) {
        this.status = status;
    }

    public UUID getDecididoPor() {
        return decididoPor;
    }

    public OffsetDateTime getDecididoEm() {
        return decididoEm;
    }

    public String getObservacaoDecisao() {
        return observacaoDecisao;
    }

    public LocalDate getPrevisaoChegada() {
        return previsaoChegada;
    }

    /**
     * Registra a decisão de quem aprovou ou recusou.
     */
    public void decidir(StatusRequisicao novoStatus, UUID autor, OffsetDateTime quando, String observacao,
            LocalDate previsao) {
        this.status = novoStatus;
        this.decididoPor = autor;
        this.decididoEm = quando.withOffsetSameInstant(ZoneOffset.UTC);
        this.observacaoDecisao = observacao;
        this.previsaoChegada = previsao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public long getVersao() {
        return versao;
    }

    public List<ItemRequisicao> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
