package br.com.canteiroconectado.util;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    public static Pageable of(Integer pagina, Integer tamanho, Sort ordem) {
        int numero = pagina == null ? 0 : pagina;
        int quantidade = tamanho == null ? TAMANHO_PADRAO : tamanho;
        if (numero < 0) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "A página deve ser 0 ou maior.");
        }
        if (quantidade < 1 || quantidade > TAMANHO_MAXIMO) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA,
                "O tamanho da página deve ficar entre 1 e " + TAMANHO_MAXIMO + ".");
        }
        return PageRequest.of(numero, quantidade, ordem);
    }

    public static Pageable of(Integer pagina, Integer tamanho) {
        return of(pagina, tamanho, Sort.unsorted());
    }

    public static String termoBusca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        return termo.strip().toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
