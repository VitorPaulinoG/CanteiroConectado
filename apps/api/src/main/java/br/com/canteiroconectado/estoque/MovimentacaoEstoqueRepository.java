package br.com.canteiroconectado.estoque;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    boolean existsByItemId(UUID itemId);
}
