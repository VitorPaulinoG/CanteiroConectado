package br.com.canteiroconectado.requisicao;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Passo da linha do tempo: o status alcançado, quem o causou, quando e com qual observação.
 */
public record EventoDaRequisicaoResponse(
    StatusRequisicao status,
    UUID usuarioId,
    String usuarioNome,
    String observacao,
    OffsetDateTime criadoEm
) {
}
