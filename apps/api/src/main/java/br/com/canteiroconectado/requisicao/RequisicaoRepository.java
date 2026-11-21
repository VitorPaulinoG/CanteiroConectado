package br.com.canteiroconectado.requisicao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface RequisicaoRepository extends JpaRepository<Requisicao, UUID> {

    /**
     * Lê a requisição com bloqueio de escrita, para duas decisões simultâneas não passarem juntas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Requisicao r where r.id = :id")
    Optional<Requisicao> travar(UUID id);
}
