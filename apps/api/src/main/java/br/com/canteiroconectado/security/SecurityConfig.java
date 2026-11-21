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

import br.com.canteiroconectado.config.JwtProperties;
import br.com.canteiroconectado.filter.UsuarioMdcFilter;
import br.com.canteiroconectado.filter.UsuarioSituacaoFilter;
import br.com.canteiroconectado.repository.usuario.UsuarioRepository;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

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
    private final SecurityErrorHandler errosSeguranca;
    private final UsuarioRepository usuarios;
    private final HandlerExceptionResolver resolvedor;

    public SecurityConfig(JwtProperties propriedades, SecurityErrorHandler errosSeguranca,
            UsuarioRepository usuarios,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedor) {
        this.propriedades = propriedades;
        this.errosSeguranca = errosSeguranca;
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
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacao()))
                .authenticationEntryPoint(errosSeguranca)
                .accessDeniedHandler(errosSeguranca))
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(errosSeguranca)
                .accessDeniedHandler(errosSeguranca))
            .addFilterAfter(new UsuarioMdcFilter(), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new UsuarioSituacaoFilter(usuarios, resolvedor), UsuarioMdcFilter.class);
        return http.build();
    }

    private static JwtAuthenticationConverter conversorAutenticacao() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(new JwtPapelConverter());
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
