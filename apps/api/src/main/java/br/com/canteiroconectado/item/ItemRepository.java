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
        order by lower(i.descricao), lower(i.codigo)
        """)
    Page<Item> listar(TipoItem tipo, Boolean ativo, Pageable pagina);
}
