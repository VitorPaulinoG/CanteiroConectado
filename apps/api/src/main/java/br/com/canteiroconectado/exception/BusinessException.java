package br.com.canteiroconectado.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Violação de regra de negócio. Vira uma resposta Problem Details com o {@code codigo} estável e,
 * quando houver, propriedades extras (por exemplo {@code bloqueadoAte}).
 */
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

    /**
     * Acrescenta uma propriedade ao corpo da resposta de erro.
     */
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
