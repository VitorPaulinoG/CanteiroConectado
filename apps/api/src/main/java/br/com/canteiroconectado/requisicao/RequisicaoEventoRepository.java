package br.com.canteiroconectado.requisicao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisicaoEventoRepository extends JpaRepository<RequisicaoEvento, UUID> {

    List<RequisicaoEvento> findByRequisicaoIdOrderByCriadoEmAsc(UUID requisicaoId);
}
