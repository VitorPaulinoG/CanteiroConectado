package br.com.canteiroconectado.repository.canteiro;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.canteiroconectado.domain.canteiro.Canteiro;

public interface CanteiroRepository extends JpaRepository<Canteiro, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    List<Canteiro> findByAtivo(boolean ativo, Sort ordem);
}
