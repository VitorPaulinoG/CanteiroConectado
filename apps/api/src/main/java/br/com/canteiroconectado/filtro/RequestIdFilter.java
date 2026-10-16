package br.com.canteiroconectado.filtro;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger ACESSO = LoggerFactory.getLogger("acesso");

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String requestId = resolverRequestId(requisicao);
        MDC.put(MDC_KEY, requestId);
        resposta.setHeader(HEADER, requestId);
        long inicioEm = System.currentTimeMillis();
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            long duracaoMs = System.currentTimeMillis() - inicioEm;
            ACESSO.info("{} {} {} {}ms", requisicao.getMethod(), requisicao.getRequestURI(),
                resposta.getStatus(), duracaoMs);
            MDC.remove(MDC_KEY);
        }
    }

    private static String resolverRequestId(HttpServletRequest requisicao) {
        String recebido = requisicao.getHeader(HEADER);
        if (recebido != null && !recebido.isBlank()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
