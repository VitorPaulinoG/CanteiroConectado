package br.com.canteiroconectado.repository.auditoria;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.canteiroconectado.domain.auditoria.LogAuditoria;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    List<LogAuditoria> findByEntidadeIdOrderByCriadoEmAsc(UUID entidadeId);
}
