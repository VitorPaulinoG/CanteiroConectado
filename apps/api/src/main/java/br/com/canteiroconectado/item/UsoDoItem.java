package br.com.canteiroconectado.item;

import java.util.UUID;

/**
 * Diz se um item já foi usado em alguma movimentação. Implementada pela funcionalidade de
 * estoque, que conhece o livro de movimentações; o catálogo só depende desta interface, então a
 * regra de dependência ({@code estoque} usa {@code item}, nunca o contrário) continua valendo.
 */
public interface UsoDoItem {

    boolean jaMovimentado(UUID itemId);
}
