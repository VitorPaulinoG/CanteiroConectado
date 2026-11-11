package br.com.canteiroconectado.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.canteiroconectado.config.JwtProperties;
import br.com.canteiroconectado.usuario.UsuarioRepository;

/**
 * Resource server com JWT simétrico (HS256) assinado com o {@code JWT_SEGREDO} (32 caracteres ou
 * mais, validado em {@link JwtProperties}). São públicos o login, o ping, o health do Actuator e o
 * Swagger; o resto exige token válido. As falhas de autenticação e de autorização saem em Problem
 * Details com {@code NAO_AUTENTICADO} (401) e {@code ACESSO_NEGADO} (403).
 */
@Configuration
@EnableMethodSecurity
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
    private final ErrosDeSegurancaHandler errosDeSeguranca;
    private final UsuarioRepository usuarios;
    private final HandlerExceptionResolver resolvedor;

    public SecurityConfig(JwtProperties propriedades, ErrosDeSegurancaHandler errosDeSeguranca,
            UsuarioRepository usuarios,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedor) {
        this.propriedades = propriedades;
        this.errosDeSeguranca = errosDeSeguranca;
        this.usuarios = usuarios;
        this.resolvedor = resolvedor;
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
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeAutenticacao()))
                .authenticationEntryPoint(errosDeSeguranca)
                .accessDeniedHandler(errosDeSeguranca))
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(errosDeSeguranca)
                .accessDeniedHandler(errosDeSeguranca))
            .addFilterAfter(new UsuarioNoMdcFilter(), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new SituacaoDoUsuarioFilter(usuarios, resolvedor), UsuarioNoMdcFilter.class);
        return http.build();
    }

    private static JwtAuthenticationConverter conversorDeAutenticacao() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(new PapeisDoJwtConverter());
        return conversor;
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
