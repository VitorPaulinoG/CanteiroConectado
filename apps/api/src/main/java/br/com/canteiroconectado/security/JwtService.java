package br.com.canteiroconectado.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.canteiroconectado.config.JwtProperties;
import br.com.canteiroconectado.domain.usuario.Usuario;

@Service
public class JwtService {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_ADMINISTRADOR = "administrador";
    public static final String CLAIM_CANTEIRO = "canteiroId";

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;
    private final Clock relogio;

    public JwtService(JwtEncoder encoder, JwtProperties propriedades, Clock relogio) {
        this.encoder = encoder;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenGerado gerarToken(Usuario usuario) {
        Instant agora = Instant.now(relogio);
        Instant expiraEm = agora.plus(propriedades.validadeHoras(), ChronoUnit.HOURS);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .subject(usuario.getId().toString())
            .issuedAt(agora)
            .expiresAt(expiraEm)
            .claim(CLAIM_PERFIL, usuario.getPerfil().name())
            .claim(CLAIM_ADMINISTRADOR, usuario.isAdministrador());

        UUID canteiroId = usuario.getCanteiroId();
        if (canteiroId != null) {
            claims.claim(CLAIM_CANTEIRO, canteiroId.toString());
        }

        String valor = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims.build())).getTokenValue();
        return new TokenGerado(valor, expiraEm);
    }

    public record TokenGerado(String valor, Instant expiraEm) {
    }
}
