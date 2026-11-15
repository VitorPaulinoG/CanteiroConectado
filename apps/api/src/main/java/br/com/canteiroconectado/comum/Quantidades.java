package br.com.canteiroconectado.comum;

import java.math.BigDecimal;

/**
 * Formatação das quantidades ({@code NUMERIC(14,3)}) devolvidas pela API.
 */
public final class Quantidades {

    private Quantidades() {
    }

    /**
     * Remove os zeros à direita sem cair em notação científica: {@code 10.000} vira {@code 10} e
     * {@code 2.500} vira {@code 2.5}.
     */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
