package br.com.canteiroconectado.repository.estoque;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.canteiroconectado.domain.estoque.MovimentacaoEstoque;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    boolean existsByItemId(UUID itemId);

    boolean existsByEstornaId(UUID estornaId);
}
