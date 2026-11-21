package br.com.canteiroconectado.domain.estoque;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarMovimentacaoCommand(
    UUID id,
    UUID canteiroId,
    UUID itemId,
    TipoMovimentacao tipo,
    MotivoMovimentacao motivo,
    BigDecimal quantidade,
    UUID colaboradorId,
    String frenteObra,
    String observacao,
    UUID estornaId,
    UUID registradoPor
) {
}
