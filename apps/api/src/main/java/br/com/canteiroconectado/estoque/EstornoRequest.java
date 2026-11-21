package br.com.canteiroconectado.estoque;

import jakarta.validation.constraints.Size;

/**
 * Pedido de estorno de uma movimentação lançada por engano.
 *
 * @param id UUID gerado no app ao abrir o diálogo; repetir o envio com o mesmo id devolve o estorno
 *     já gravado
 * @param motivo por que o lançamento está errado; obrigatório
 */
public record EstornoRequest(
    String id,

    @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres")
    String motivo
) {

    @Override
    public String toString() {
        return "EstornoRequest[id=" + id + "]";
    }
}
