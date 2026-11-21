package br.com.canteiroconectado.canteiro;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteiroRepository extends JpaRepository<Canteiro, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    List<Canteiro> findByAtivo(boolean ativo, Sort ordem);
}
