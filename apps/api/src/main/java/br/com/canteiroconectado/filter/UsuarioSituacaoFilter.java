package br.com.canteiroconectado.filter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import br.com.canteiroconectado.dominio.usuario.UsuarioSituacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.usuario.UsuarioRepository;
import br.com.canteiroconectado.security.UsuarioLogado;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UsuarioSituacaoFilter extends OncePerRequestFilter {

    static final Set<String> LIBERADAS_NA_TROCA = Set.of(
        "GET /api/v1/autenticacao/eu",
        "POST /api/v1/autenticacao/trocar-senha");

    private final UsuarioRepository usuarios;
    private final HandlerExceptionResolver resolvedor;

    public UsuarioSituacaoFilter(UsuarioRepository usuarios, HandlerExceptionResolver resolvedor) {
        this.usuarios = usuarios;
        this.resolvedor = resolvedor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token)) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        UsuarioLogado doToken = UsuarioLogado.of(token.getToken());
        Optional<UsuarioSituacao> situacao = usuarios.buscarSituacao(doToken.id());
        if (situacao.isEmpty() || !situacao.get().ativo() || mudouDesdeOLogin(doToken, situacao.get())) {
            SecurityContextHolder.clearContext();
            resolvedor.resolveException(requisicao, resposta, null,
                new InsufficientAuthenticationException("Usuário desativado, inexistente ou alterado."));
            return;
        }
        if (situacao.get().precisaTrocarSenha() && !situacao.get().administrador()
                && !LIBERADAS_NA_TROCA.contains(requisicao.getMethod() + " " + requisicao.getRequestURI())) {
            resolvedor.resolveException(requisicao, resposta, null,
                new BusinessException(ErrorCode.TROCA_SENHA_OBRIGATORIA,
                    "Troque a senha temporária para continuar."));
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static boolean mudouDesdeOLogin(UsuarioLogado doToken, UsuarioSituacao atual) {
        return doToken.perfil() != atual.perfil()
            || doToken.administrador() != atual.administrador()
            || !Objects.equals(doToken.canteiroId(), atual.canteiroId());
    }
}
