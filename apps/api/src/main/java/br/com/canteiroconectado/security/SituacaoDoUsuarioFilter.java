package br.com.canteiroconectado.security;

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

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.usuario.SituacaoDoUsuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Confere no banco, a cada requisição autenticada, se o dono do token continua ativo, se o perfil
 * e o canteiro ainda são os do token e se ainda precisa trocar a senha. Token de usuário
 * desativado ou alterado pelo administrador recebe 401, e o app pede novo login. Funcionário (não administrador)
 * com troca obrigatória só acessa {@code GET /autenticacao/eu} e {@code POST /autenticacao/trocar-senha};
 * o resto recebe 403 {@code TROCA_DE_SENHA_OBRIGATORIA}.
 */
class SituacaoDoUsuarioFilter extends OncePerRequestFilter {

    static final Set<String> LIBERADAS_NA_TROCA = Set.of(
        "GET /api/v1/autenticacao/eu",
        "POST /api/v1/autenticacao/trocar-senha");

    private final UsuarioRepository usuarios;
    private final HandlerExceptionResolver resolvedor;

    SituacaoDoUsuarioFilter(UsuarioRepository usuarios, HandlerExceptionResolver resolvedor) {
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

        UsuarioLogado doToken = UsuarioLogado.de(token.getToken());
        Optional<SituacaoDoUsuario> situacao = usuarios.buscarSituacao(doToken.id());
        if (situacao.isEmpty() || !situacao.get().ativo() || mudouDesdeOLogin(doToken, situacao.get())) {
            SecurityContextHolder.clearContext();
            resolvedor.resolveException(requisicao, resposta, null,
                new InsufficientAuthenticationException("Usuário desativado, inexistente ou alterado."));
            return;
        }
        if (situacao.get().precisaTrocarSenha() && !situacao.get().administrador()
                && !LIBERADAS_NA_TROCA.contains(requisicao.getMethod() + " " + requisicao.getRequestURI())) {
            resolvedor.resolveException(requisicao, resposta, null,
                new BusinessException(ErrorCode.TROCA_DE_SENHA_OBRIGATORIA,
                    "Troque a senha temporária para continuar."));
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static boolean mudouDesdeOLogin(UsuarioLogado doToken, SituacaoDoUsuario atual) {
        return doToken.perfil() != atual.perfil()
            || doToken.administrador() != atual.administrador()
            || !Objects.equals(doToken.canteiroId(), atual.canteiroId());
    }
}
