package br.com.canteiroconectado.excecao;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ErrorCode codigo;
    private final HttpStatus status;

    public BusinessException(ErrorCode codigo, String mensagem) {
        this(codigo, codigo.statusPadrao(), mensagem);
    }

    public BusinessException(ErrorCode codigo, HttpStatus status, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public ErrorCode codigo() {
        return codigo;
    }

    public HttpStatus status() {
        return status;
    }
}
