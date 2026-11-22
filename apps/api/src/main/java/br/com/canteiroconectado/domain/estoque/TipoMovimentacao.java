package br.com.canteiroconectado.domain.estoque;

import java.math.BigDecimal;

public enum TipoMovimentacao {
    ENTRADA,
    SAIDA;

    public BigDecimal aplicar(BigDecimal saldo, BigDecimal quantidade) {
        return this == ENTRADA ? saldo.add(quantidade) : saldo.subtract(quantidade);
    }

    public TipoMovimentacao contrario() {
        return this == ENTRADA ? SAIDA : ENTRADA;
    }
}
