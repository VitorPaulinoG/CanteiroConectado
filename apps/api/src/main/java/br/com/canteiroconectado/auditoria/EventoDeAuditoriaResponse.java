package br.com.canteiroconectado.auditoria;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Evento da trilha de auditoria (S18), sem senha nem token.
 *
 * @param usuarioNome autor da ação; nulo para ações do sistema ou de quem não entrou
 * @param entidadeDescricao nome legível do alvo (funcionário, canteiro, item, requisição...)
 * @param requestId identificador da requisição HTTP, para achar as linhas de log
 */
public record EventoDeAuditoriaResponse(
    UUID id,
    AcaoAuditoria acao,
    UUID usuarioId,
    String usuarioNome,
    String entidade,
    UUID entidadeId,
    String entidadeDescricao,
    Map<String, Object> detalhes,
    String requestId,
    OffsetDateTime criadoEm
) {
}
