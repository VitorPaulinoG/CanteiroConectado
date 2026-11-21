package br.com.canteiroconectado.usuario;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário que acessa a API (ALMOXARIFE ou GESTOR). Nunca é excluído, apenas desativado.
 * O e-mail é guardado sem espaços nas pontas e em minúsculas.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false)
    private Perfil perfil;

    @Column(name = "administrador", nullable = false)
    private boolean administrador;

    @Column(name = "canteiro_id")
    private UUID canteiroId;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "senha_temporaria_cifrada")
    private String senhaTemporariaCifrada;

    @Column(name = "precisa_trocar_senha", nullable = false)
    private boolean precisaTrocarSenha;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private OffsetDateTime bloqueadoAte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() {
    }

    /**
     * Cria um usuário ativo com id e data de criação gerados. ALMOXARIFE exige canteiro e GESTOR
     * não tem canteiro, as mesmas regras do {@code CHECK} da migration V2.
     */
    public Usuario(String nome, String email, String senhaHash, Perfil perfil,
            boolean administrador, UUID canteiroId) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.administrador = administrador;
        this.canteiroId = canteiroId;
        this.ativo = true;
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Normaliza o e-mail para gravação e comparação: sem espaços nas pontas e em minúsculas.
     */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizarEmail(email);
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public void setAdministrador(boolean administrador) {
        this.administrador = administrador;
    }

    public UUID getCanteiroId() {
        return canteiroId;
    }

    public void setCanteiroId(UUID canteiroId) {
        this.canteiroId = canteiroId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getSenhaTemporariaCifrada() {
        return senhaTemporariaCifrada;
    }

    public void setSenhaTemporariaCifrada(String senhaTemporariaCifrada) {
        this.senhaTemporariaCifrada = senhaTemporariaCifrada;
    }

    public boolean isPrecisaTrocarSenha() {
        return precisaTrocarSenha;
    }

    public void setPrecisaTrocarSenha(boolean precisaTrocarSenha) {
        this.precisaTrocarSenha = precisaTrocarSenha;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public OffsetDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(OffsetDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
