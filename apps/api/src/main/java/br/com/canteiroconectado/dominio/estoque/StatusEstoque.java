package br.com.canteiroconectado.dominio.estoque;

import java.math.BigDecimal;

public enum StatusEstoque {
    ZERADO,
    ABAIXO_DO_MINIMO,
    NORMAL;

    public static StatusEstoque of(BigDecimal saldo, BigDecimal minimo) {
        if (minimo.signum() > 0 && saldo.signum() == 0) {
            return ZERADO;
        }
        if (saldo.signum() > 0 && saldo.compareTo(minimo) < 0) {
            return ABAIXO_DO_MINIMO;
        }
        return NORMAL;
    }
}
