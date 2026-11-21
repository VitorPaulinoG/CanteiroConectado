package br.com.canteiroconectado.comum;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Página de resultados devolvida pelas listas paginadas.
 *
 * @param pagina índice da página, começando em 0
 * @param tamanho quantidade máxima de itens por página
 */
public record PaginaResponse<T>(
    List<T> conteudo,
    int pagina,
    int tamanho,
    long totalElementos,
    int totalPaginas
) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResponse<>(pagina.getContent().stream().map(conversor).toList(),
            pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
