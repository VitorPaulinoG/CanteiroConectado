package br.com.canteiroconectado.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.canteiroconectado.security.JwtService;
import br.com.canteiroconectado.usuario.Perfil;

/**
 * Executa uma ação com a identidade de um usuário, para a carga de demonstração passar pelos
 * mesmos services, regras e permissões que o app usa. O contexto de segurança anterior é
 * restaurado no fim.
 */
final class ComoUsuario {

    record Pessoa(UUID id, Perfil perfil, boolean administrador, UUID canteiroId) {
    }

    private ComoUsuario() {
    }

    static <T> T executar(Pessoa pessoa, Supplier<T> acao) {
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao(pessoa));
        SecurityContextHolder.setContext(contexto);
        try {
            return acao.get();
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    private static JwtAuthenticationToken autenticacao(Pessoa pessoa) {
        Instant agora = Instant.now();
        Jwt.Builder jwt = Jwt.withTokenValue("carga-de-demonstracao")
            .header("alg", "none")
            .subject(pessoa.id().toString())
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(600))
            .claim(JwtService.CLAIM_PERFIL, pessoa.perfil().name())
            .claim(JwtService.CLAIM_ADMINISTRADOR, pessoa.administrador());
        if (pessoa.canteiroId() != null) {
            jwt.claim(JwtService.CLAIM_CANTEIRO, pessoa.canteiroId().toString());
        }
        List<GrantedAuthority> papeis = new ArrayList<>();
        papeis.add(new SimpleGrantedAuthority("ROLE_" + pessoa.perfil().name()));
        if (pessoa.administrador()) {
            papeis.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new JwtAuthenticationToken(jwt.build(), papeis);
    }
}
