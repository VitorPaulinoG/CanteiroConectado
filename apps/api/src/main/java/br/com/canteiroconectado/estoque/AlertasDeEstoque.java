package br.com.canteiroconectado.estoque;

import java.util.List;

/**
 * Alertas de estoque do canteiro.
 *
 * @param maisCriticos itens zerados primeiro, depois os abaixo do mínimo pela menor proporção
 *     entre saldo e mínimo
 */
public record AlertasDeEstoque(long zerados, long abaixoDoMinimo, List<ItemEstoqueResponse> maisCriticos) {
}
