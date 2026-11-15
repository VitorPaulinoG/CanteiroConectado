package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dados de uma movimentação a aplicar no livro e no saldo.
 *
 * @param id identificador gerado no app (ou pelo servidor, no estorno)
 * @param estornaId movimentação corrigida, só quando o motivo é {@code ESTORNO}
 * @param registradoPor usuário autor do registro
 */
public record NovaMovimentacao(
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
