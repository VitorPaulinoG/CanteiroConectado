package br.com.canteiroconectado.repository.requisicao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import br.com.canteiroconectado.dominio.requisicao.Requisicao;

import jakarta.persistence.LockModeType;

public interface RequisicaoRepository extends JpaRepository<Requisicao, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Requisicao r where r.id = :id")
    Optional<Requisicao> travar(UUID id);
}
