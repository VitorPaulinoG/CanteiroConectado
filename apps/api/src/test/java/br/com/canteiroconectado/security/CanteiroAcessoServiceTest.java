package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

@DisplayName("CanteiroAcessoService")
class CanteiroAcessoServiceTest {

    private static final UUID CANTEIRO_A = UUID.randomUUID();
    private static final UUID CANTEIRO_B = UUID.randomUUID();

    private final CanteiroAcessoService acesso = new CanteiroAcessoService();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void logar(String perfil, UUID canteiroId) {
        Jwt.Builder jwt = Jwt.withTokenValue("t")
            .header("alg", "HS256")
            .subject(UUID.randomUUID().toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim(JwtService.CLAIM_PERFIL, perfil)
            .claim(JwtService.CLAIM_ADMINISTRADOR, false);
        if (canteiroId != null) {
            jwt.claim(JwtService.CLAIM_CANTEIRO, canteiroId.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build()));
    }

    @Nested
    @DisplayName("conferir")
    class Conferir {

        @Test
        @DisplayName("deve liberar o ALMOXARIFE no próprio canteiro")
        void deve_liberar_quando_almoxarifeNoProprioCanteiro() {
            logar("ALMOXARIFE", CANTEIRO_A);
            acesso.conferir(CANTEIRO_A);
            assertThat(acesso.pode(CANTEIRO_A)).isTrue();
        }

        @Test
        @DisplayName("deve recusar com ACESSO_NEGADO o ALMOXARIFE em outro canteiro")
        void deve_recusar_quando_almoxarifeEmOutroCanteiro() {
            logar("ALMOXARIFE", CANTEIRO_A);
            assertThatThrownBy(() -> acesso.conferir(CANTEIRO_B))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).codigo())
                .isEqualTo(ErrorCode.ACESSO_NEGADO);
        }

        @Test
        @DisplayName("deve liberar o GESTOR em qualquer canteiro")
        void deve_liberar_quando_gestor() {
            logar("GESTOR", null);
            acesso.conferir(CANTEIRO_A);
            acesso.conferir(CANTEIRO_B);
        }
    }

    @Nested
    @DisplayName("resolver")
    class Resolver {

        @Test
        @DisplayName("deve usar o canteiro do ALMOXARIFE quando nenhum é informado")
        void deve_usarProprioCanteiro_quando_almoxarifeSemCanteiro() {
            logar("ALMOXARIFE", CANTEIRO_A);
            assertThat(acesso.resolver(null)).isEqualTo(CANTEIRO_A);
        }

        @Test
        @DisplayName("deve exigir o canteiro do GESTOR")
        void deve_exigirCanteiro_quando_gestorSemCanteiro() {
            logar("GESTOR", null);
            assertThatThrownBy(() -> acesso.resolver(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).codigo())
                .isEqualTo(ErrorCode.VALIDACAO_INVALIDA);
            assertThat(acesso.resolver(CANTEIRO_B)).isEqualTo(CANTEIRO_B);
        }
    }
}
