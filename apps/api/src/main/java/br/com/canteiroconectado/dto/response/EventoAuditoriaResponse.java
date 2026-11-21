package br.com.canteiroconectado.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import br.com.canteiroconectado.domain.auditoria.AcaoAuditoria;

public record EventoAuditoriaResponse(
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
