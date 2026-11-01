package br.com.canteiroconectado.comum;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

/**
 * Converte os parâmetros {@code pagina} e {@code tamanho} das listas em {@link Pageable}.
 * A primeira página é 0; o tamanho padrão é 20 e o máximo, 100.
 */
public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    /**
     * @throws BusinessException {@code VALIDACAO_INVALIDA} com página negativa ou tamanho fora de 1 a 100
     */
    public static Pageable de(Integer pagina, Integer tamanho, Sort ordem) {
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

    public static Pageable de(Integer pagina, Integer tamanho) {
        return de(pagina, tamanho, Sort.unsorted());
    }

    /**
     * Prepara um termo de busca para {@code LIKE}: sem espaços nas pontas, em minúsculas e com
     * {@code %}, {@code _} e {@code \} escapados. Devolve nulo para termo vazio.
     */
    public static String termoDeBusca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        return termo.strip().toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
