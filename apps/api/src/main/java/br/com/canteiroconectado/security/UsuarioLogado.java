package br.com.canteiroconectado.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.canteiroconectado.dominio.usuario.Perfil;

public record UsuarioLogado(UUID id, Perfil perfil, boolean administrador, UUID canteiroId) {

    public static UsuarioLogado atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            return of(token.getToken());
        }
        throw new AuthenticationCredentialsNotFoundException("Nenhum usuário autenticado.");
    }

    public static UsuarioLogado of(Jwt jwt) {
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
