package br.com.canteiroconectado.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.canteiroconectado.usuario.Perfil;

/**
 * Usuário da requisição atual, lido das claims do JWT validado.
 *
 * @param canteiroId canteiro do ALMOXARIFE; nulo para GESTOR
 */
public record UsuarioLogado(UUID id, Perfil perfil, boolean administrador, UUID canteiroId) {

    /**
     * Lê o usuário do contexto de segurança da requisição.
     *
     * @throws AuthenticationCredentialsNotFoundException se não houver usuário autenticado
     */
    public static UsuarioLogado atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            return de(token.getToken());
        }
        throw new AuthenticationCredentialsNotFoundException("Nenhum usuário autenticado.");
    }

    /**
     * Converte as claims de um JWT emitido pelo {@link JwtService}.
     */
    public static UsuarioLogado de(Jwt jwt) {
        String canteiro = jwt.getClaimAsString(JwtService.CLAIM_CANTEIRO);
        return new UsuarioLogado(
            UUID.fromString(jwt.getSubject()),
            Perfil.valueOf(jwt.getClaimAsString(JwtService.CLAIM_PERFIL)),
            Boolean.TRUE.equals(jwt.getClaimAsBoolean(JwtService.CLAIM_ADMINISTRADOR)),
            canteiro == null ? null : UUID.fromString(canteiro));
    }

    public boolean isAlmoxarife() {
        return perfil == Perfil.ALMOXARIFE;
    }

    public boolean isGestor() {
        return perfil == Perfil.GESTOR;
    }
}
