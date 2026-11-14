package br.com.canteiroconectado.colaborador;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ColaboradorRepository extends JpaRepository<Colaborador, UUID> {

    boolean existsByCanteiroIdAndNomeIgnoreCase(UUID canteiroId, String nome);

    boolean existsByCanteiroIdAndNomeIgnoreCaseAndIdNot(UUID canteiroId, String nome, UUID id);

    @Query("""
        select c from Colaborador c
        where c.canteiroId = :canteiroId
          and (:ativo is null or c.ativo = :ativo)
          and (:termo is null or lower(c.nome) like concat('%', cast(:termo as string), '%') escape '\\')
        order by
          case when :termo is not null and lower(c.nome) like concat(cast(:termo as string), '%') escape '\\' then 0 else 1 end,
          lower(c.nome)
        """)
    Page<Colaborador> buscar(UUID canteiroId, String termo, Boolean ativo, Pageable pagina);
}
