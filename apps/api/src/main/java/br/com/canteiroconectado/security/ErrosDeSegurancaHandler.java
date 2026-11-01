package br.com.canteiroconectado.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import br.com.canteiroconectado.exception.GlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde às falhas da cadeia de filtros do Spring Security (sem token, token inválido, papel
 * insuficiente) no mesmo formato Problem Details do {@link GlobalExceptionHandler}, porque essas
 * falhas acontecem antes de chegar a um controller.
 */
@Component
public class ErrosDeSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolvedor;

    public ErrosDeSegurancaHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedor) {
        this.resolvedor = resolvedor;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
            AuthenticationException excecao) {
        resolvedor.resolveException(requisicao, resposta, null, excecao);
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
            AccessDeniedException excecao) {
        resolvedor.resolveException(requisicao, resposta, null, excecao);
    }
}
