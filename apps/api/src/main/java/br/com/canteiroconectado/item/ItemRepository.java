package br.com.canteiroconectado.item;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, UUID id);

    @Query("""
        select i from Item i
        where (:tipo is null or i.tipo = :tipo)
          and (:ativo is null or i.ativo = :ativo)
          and (:termo is null
               or lower(i.codigo) like concat('%', cast(:termo as string), '%') escape '\\'
               or lower(i.descricao) like concat('%', cast(:termo as string), '%') escape '\\')
        order by
          case
            when :termo is null then 0
            when lower(i.codigo) like concat(cast(:termo as string), '%') escape '\\' then 0
            when lower(i.descricao) like concat(cast(:termo as string), '%') escape '\\' then 1
            else 2
          end,
          lower(i.descricao),
          lower(i.codigo)
        """)
    Page<Item> buscar(String termo, TipoItem tipo, Boolean ativo, Pageable pagina);
}
