package br.com.canteiroconectado.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.canteiroconectado.config.JwtProperties;

/**
 * Resource server com JWT simétrico (HS256) assinado com o {@code JWT_SEGREDO} (32 caracteres ou
 * mais, validado em {@link JwtProperties}). São públicos o login, o ping, o health do Actuator e o
 * Swagger; o resto exige token válido.
 */
@Configuration
public class SecurityConfig {

    static final String[] ROTAS_PUBLICAS = {
        "/api/v1/ping",
        "/actuator/health",
        "/actuator/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final JwtProperties propriedades;

    public SecurityConfig(JwtProperties propriedades) {
        this.propriedades = propriedades;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requisicoes -> requisicoes
                .requestMatchers(HttpMethod.POST, "/api/v1/autenticacao/login").permitAll()
                .requestMatchers(HttpMethod.GET, ROTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }));
        return http.build();
    }

    private SecretKey chaveHmac() {
        byte[] bytes = propriedades.segredo().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveHmac()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveHmac())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
