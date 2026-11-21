package br.com.canteiroconectado.colaborador;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pessoa que retira material no canteiro, sem login no app. Nunca é excluída, só desativada; o
 * nome é único no canteiro sem diferenciar maiúsculas.
 */
@Entity
@Table(name = "colaborador")
public class Colaborador {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "canteiro_id", nullable = false, updatable = false)
    private UUID canteiroId;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected Colaborador() {
    }

    public Colaborador(String nome, UUID canteiroId) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.canteiroId = canteiroId;
        this.ativo = true;
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public UUID getCanteiroId() {
        return canteiroId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
