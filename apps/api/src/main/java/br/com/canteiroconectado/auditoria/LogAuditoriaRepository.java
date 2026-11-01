package br.com.canteiroconectado.auditoria;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    List<LogAuditoria> findByEntidadeIdOrderByCriadoEmAsc(UUID entidadeId);
}
