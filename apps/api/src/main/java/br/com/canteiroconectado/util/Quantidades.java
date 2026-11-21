package br.com.canteiroconectado.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

public final class Quantidades {

    public static final int CASAS_DECIMAIS = 3;

    private Quantidades() {
    }

    public static BigDecimal validar(BigDecimal quantidade, String campo) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw invalida("A quantidade deve ser maior que zero.", campo);
        }
        if (quantidade.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw invalida("A quantidade aceita no máximo 3 casas decimais.", campo);
        }
        BigDecimal ajustada = quantidade.setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY);
        if (ajustada.precision() > 14) {
            throw invalida("A quantidade é grande demais.", campo);
        }
        return ajustada;
    }

    private static BusinessException invalida(String mensagem, String campo) {
        return new BusinessException(ErrorCode.QUANTIDADE_INVALIDA, mensagem).com("campo", campo);
    }

    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
