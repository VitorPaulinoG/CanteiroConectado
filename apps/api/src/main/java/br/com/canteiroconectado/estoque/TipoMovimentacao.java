package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;

/**
 * Sentido da movimentação: a quantidade é sempre positiva e o tipo diz se soma ou subtrai.
 */
public enum TipoMovimentacao {
    ENTRADA,
    SAIDA;

    /**
     * Aplica a quantidade ao saldo no sentido deste tipo.
     */
    public BigDecimal aplicar(BigDecimal saldo, BigDecimal quantidade) {
        return this == ENTRADA ? saldo.add(quantidade) : saldo.subtract(quantidade);
    }

    public TipoMovimentacao contrario() {
        return this == ENTRADA ? SAIDA : ENTRADA;
    }
}
