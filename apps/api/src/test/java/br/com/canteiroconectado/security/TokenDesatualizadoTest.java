package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("Token desatualizado e identidade")
class TokenDesatualizadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private UsuarioRepository usuarios;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("SituacaoDoUsuarioFilter")
    class Filtro {

        @Test
        @DisplayName("deve devolver 401 ao token antigo quando o perfil do funcionário muda")
        void deve_devolver401_quando_perfilMuda() throws Exception {
            Usuario funcionario = tokens.almoxarife();
            String antigo = tokens.bearer(funcionario);
            funcionario.setPerfil(Perfil.GESTOR);
            funcionario.setCanteiroId(null);
            usuarios.saveAndFlush(funcionario);

            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", antigo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", tokens.bearer(funcionario)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve devolver 401 ao token antigo quando o funcionário perde a administração")
        void deve_devolver401_quando_deixaDeSerAdministrador() throws Exception {
            Usuario admin = tokens.admin();
            String antigo = tokens.bearer(admin);
            admin.setAdministrador(false);
            usuarios.saveAndFlush(admin);

            mockMvc.perform(get("/api/v1/usuarios").header("Authorization", antigo))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve devolver 401 ao token assinado de um usuário que não existe no banco")
        void deve_devolver401_quando_usuarioInexistente() throws Exception {
            Usuario fantasma = new Usuario("Fantasma", "fantasma@canteiro.test", "x", Perfil.GESTOR, false, null);
            fantasma.setId(UUID.randomUUID());

            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", tokens.bearer(fantasma)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("identidade no código")
    class Identidade {

        private Jwt jwt(String perfil) {
            Jwt.Builder jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim(JwtService.CLAIM_ADMINISTRADOR, false);
            if (perfil != null) {
                jwt.claim(JwtService.CLAIM_PERFIL, perfil);
            }
            return jwt.build();
        }

        @Test
        @DisplayName("deve falhar ao ler o usuário logado quando não há autenticação")
        void deve_falhar_quando_semAutenticacao() {
            assertThatThrownBy(UsuarioLogado::atual).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }

        @Test
        @DisplayName("não deve dar papel nenhum ao token sem perfil")
        void naoDeve_darPapel_quando_tokenSemPerfil() {
            assertThat(new PapeisDoJwtConverter().convert(jwt(null))).isEmpty();
            assertThat(new PapeisDoJwtConverter().convert(jwt("GESTOR"))).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_GESTOR");
        }

        @Test
        @DisplayName("não deve liberar canteiro nulo nem para o GESTOR")
        void naoDeve_liberar_quando_canteiroNulo() {
            UsuarioLogado gestor = new UsuarioLogado(UUID.randomUUID(), Perfil.GESTOR, false, null);

            assertThat(new CanteiroAcessoService().pode(gestor, null)).isFalse();
        }
    }
}
