package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;

/**
 * Situação do saldo de um item em relação à quantidade mínima.
 *
 * <ul>
 *   <li>{@link #ZERADO}: saldo 0 e mínimo maior que 0;</li>
 *   <li>{@link #ABAIXO_DO_MINIMO}: saldo maior que 0 e menor que o mínimo;</li>
 *   <li>{@link #NORMAL}: os demais casos, inclusive mínimo 0 ("sem alerta").</li>
 * </ul>
 *
 * A ordem das constantes é a ordem de exibição: os críticos primeiro.
 */
public enum StatusEstoque {
    ZERADO,
    ABAIXO_DO_MINIMO,
    NORMAL;

    public static StatusEstoque de(BigDecimal saldo, BigDecimal minimo) {
        if (minimo.signum() > 0 && saldo.signum() == 0) {
            return ZERADO;
        }
        if (saldo.signum() > 0 && saldo.compareTo(minimo) < 0) {
            return ABAIXO_DO_MINIMO;
        }
        return NORMAL;
    }
}
