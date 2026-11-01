package br.com.canteiroconectado.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

/**
 * Validação e formatação das quantidades ({@code NUMERIC(14,3)}) da API.
 */
public final class Quantidades {

    public static final int CASAS_DECIMAIS = 3;

    private Quantidades() {
    }

    /**
     * Confere que a quantidade é positiva e tem no máximo 3 casas decimais e a devolve com escala 3.
     *
     * @param campo nome do campo devolvido no erro, para o app marcar o campo certo
     * @throws BusinessException {@code QUANTIDADE_INVALIDA}
     */
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

    /**
     * Remove os zeros à direita sem cair em notação científica: {@code 10.000} vira {@code 10} e
     * {@code 2.500} vira {@code 2.5}.
     */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
