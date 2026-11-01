package br.com.canteiroconectado.estoque;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtro do histórico de movimentações de um canteiro. As datas são instantes em UTC; {@code de}
 * entra na consulta e {@code ate} não ({@code de <= criado_em < ate}), para o app pedir "hoje"
 * com o começo do dia e o começo do dia seguinte.
 */
record FiltroDoHistorico(
    UUID canteiroId,
    Instant de,
    Instant ate,
    TipoMovimentacao tipo,
    UUID itemId,
    UUID colaboradorId
) {
}
