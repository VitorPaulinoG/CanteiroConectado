package br.com.canteiroconectado.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ErrorCode codigo;
    private final HttpStatus status;
    private final Map<String, Object> propriedades = new LinkedHashMap<>();

    public BusinessException(ErrorCode codigo, String mensagem) {
        this(codigo, codigo.statusPadrao(), mensagem);
    }

    public BusinessException(ErrorCode codigo, HttpStatus status, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public BusinessException com(String nome, Object valor) {
        propriedades.put(nome, valor);
        return this;
    }

    public ErrorCode codigo() {
        return codigo;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, Object> propriedades() {
        return Collections.unmodifiableMap(propriedades);
    }
}
