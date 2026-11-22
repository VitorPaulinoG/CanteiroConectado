package br.com.canteiroconectado.util;

public record ResultadoGravacao<T>(T registro, boolean novo) {

    public static <T> ResultadoGravacao<T> criado(T registro) {
        return new ResultadoGravacao<>(registro, true);
    }

    public static <T> ResultadoGravacao<T> repetido(T registro) {
        return new ResultadoGravacao<>(registro, false);
    }
}
