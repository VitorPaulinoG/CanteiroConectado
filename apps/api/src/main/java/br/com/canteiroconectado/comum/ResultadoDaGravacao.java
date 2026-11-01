package br.com.canteiroconectado.comum;

/**
 * Resultado de uma gravação com id do cliente.
 *
 * @param novo verdadeiro quando o registro foi criado agora; falso quando o mesmo pedido já
 *     tinha sido gravado e o registro existente foi devolvido
 */
public record ResultadoDaGravacao<T>(T registro, boolean novo) {

    public static <T> ResultadoDaGravacao<T> criado(T registro) {
        return new ResultadoDaGravacao<>(registro, true);
    }

    public static <T> ResultadoDaGravacao<T> repetido(T registro) {
        return new ResultadoDaGravacao<>(registro, false);
    }
}
