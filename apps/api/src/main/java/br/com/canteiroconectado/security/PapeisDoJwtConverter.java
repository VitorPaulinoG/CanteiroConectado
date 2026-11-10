package br.com.canteiroconectado.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Transforma as claims {@code perfil} e {@code administrador} do JWT nos papéis usados pelo
 * {@code @PreAuthorize}.
 */
class PapeisDoJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> papeis = new ArrayList<>();
        String perfil = jwt.getClaimAsString(JwtService.CLAIM_PERFIL);
        if (perfil != null) {
            papeis.add(new SimpleGrantedAuthority("ROLE_" + perfil));
        }
        if (Boolean.TRUE.equals(jwt.getClaimAsBoolean(JwtService.CLAIM_ADMINISTRADOR))) {
            papeis.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return papeis;
    }
}
