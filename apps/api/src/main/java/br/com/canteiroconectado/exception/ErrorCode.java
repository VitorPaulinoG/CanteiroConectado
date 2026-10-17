package br.com.canteiroconectado.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDACAO_INVALIDA(HttpStatus.BAD_REQUEST),
    NAO_AUTENTICADO(HttpStatus.UNAUTHORIZED),
    ACESSO_NEGADO(HttpStatus.FORBIDDEN),
    RECURSO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
    METODO_NAO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLITO(HttpStatus.CONFLICT),
    ERRO_INESPERADO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus statusPadrao;

    ErrorCode(HttpStatus statusPadrao) {
        this.statusPadrao = statusPadrao;
    }

    public HttpStatus statusPadrao() {
        return statusPadrao;
    }
}
