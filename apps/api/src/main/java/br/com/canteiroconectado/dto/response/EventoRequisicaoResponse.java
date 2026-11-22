package br.com.canteiroconectado.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;

public record EventoRequisicaoResponse(
    StatusRequisicao status,
    UUID usuarioId,
    String usuarioNome,
    String observacao,
    OffsetDateTime criadoEm
) {
}
