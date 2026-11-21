package br.com.canteiroconectado.repository.requisicao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.canteiroconectado.dominio.requisicao.RequisicaoEvento;

public interface RequisicaoEventoRepository extends JpaRepository<RequisicaoEvento, UUID> {

    List<RequisicaoEvento> findByRequisicaoIdOrderByCriadoEmAsc(UUID requisicaoId);
}
