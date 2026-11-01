package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Movimentação com os nomes de item, canteiro, colaborador e autor, como o app mostra no
 * comprovante, no detalhe do item e no histórico.
 *
 * @param estornaId movimentação que esta corrige (só no estorno)
 * @param estornadaPorId estorno que corrigiu esta movimentação, quando houver
 */
public record MovimentacaoResponse(
    UUID id,
    UUID canteiroId,
    String canteiroNome,
    UUID itemId,
    String itemCodigo,
    String itemDescricao,
    String unidadeSimbolo,
    TipoMovimentacao tipo,
    MotivoMovimentacao motivo,
    BigDecimal quantidade,
    BigDecimal saldoApos,
    UUID colaboradorId,
    String colaboradorNome,
    String frenteObra,
    String observacao,
    UUID estornaId,
    UUID estornadaPorId,
    UUID registradoPorId,
    String registradoPorNome,
    OffsetDateTime criadoEm
) {
}
