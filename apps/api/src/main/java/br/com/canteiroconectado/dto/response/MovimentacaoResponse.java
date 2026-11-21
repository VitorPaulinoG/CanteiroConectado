package br.com.canteiroconectado.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.canteiroconectado.dominio.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.dominio.estoque.TipoMovimentacao;

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
