package br.com.canteiroconectado.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.canteiroconectado.filter.RequestIdFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Coloca o id do usuário autenticado no MDC ({@code usuarioId}) para aparecer em toda linha de log
 * da requisição, inclusive na linha de acesso do {@link RequestIdFilter}, que limpa o valor no fim.
 */
class UsuarioNoMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            MDC.put(RequestIdFilter.MDC_USUARIO, token.getToken().getSubject());
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
