package br.com.canteiroconectado.auditoria;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.filter.RequestIdFilter;

/**
 * Grava eventos na trilha de auditoria. O {@code requestId} vem do MDC preenchido pelo
 * {@link RequestIdFilter}, para ligar o evento às linhas de log da mesma requisição.
 */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository logs;
    private final Clock relogio;

    public AuditoriaService(LogAuditoriaRepository logs, Clock relogio) {
        this.logs = logs;
        this.relogio = relogio;
    }

    /**
     * Registra um evento na transação corrente: se a operação auditada for desfeita, o evento
     * também é.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LogAuditoria registrar(UUID usuarioId, AcaoAuditoria acao, String entidade, UUID entidadeId,
            Map<String, Object> detalhes) {
        return logs.save(new LogAuditoria(usuarioId, acao, entidade, entidadeId, detalhes,
            MDC.get(RequestIdFilter.MDC_KEY), OffsetDateTime.now(relogio)));
    }
}
