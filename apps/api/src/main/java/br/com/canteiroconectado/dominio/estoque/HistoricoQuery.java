package br.com.canteiroconectado.dominio.estoque;

import java.time.Instant;
import java.util.UUID;

public record HistoricoQuery(
    UUID canteiroId,
    Instant inicio,
    Instant ate,
    TipoMovimentacao tipo,
    UUID itemId,
    UUID colaboradorId
) {
}
