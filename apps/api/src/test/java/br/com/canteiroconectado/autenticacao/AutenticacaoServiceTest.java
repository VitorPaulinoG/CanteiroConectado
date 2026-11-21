package br.com.canteiroconectado.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.JwtService;
import br.com.canteiroconectado.support.RelogioAjustavel;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AutenticacaoService")
class AutenticacaoServiceTest {

    private static final String EMAIL = "sebastiao@canteiro.test";
    private static final String SENHA = "senha-certa-1";

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private PasswordEncoder encoderSenha;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditoriaService auditoria;

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-01T12:00:00Z"));

    private AutenticacaoService servico;
    private Usuario usuario;

    @BeforeEach
    void montar() {
        servico = new AutenticacaoService(usuarios, encoderSenha, jwtService, auditoria, relogio);
        usuario = new Usuario("Sebastião", EMAIL, "hash", Perfil.GESTOR, false, null);
        when(usuarios.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(encoderSenha.matches(eq(SENHA), eq("hash"))).thenReturn(true);
        when(jwtService.gerarToken(any())).thenReturn(
            new JwtService.TokenGerado("token", Instant.parse("2026-11-02T00:00:00Z")));
    }

    private LoginResponse entrar(String senha) {
        return servico.login(new LoginRequest(EMAIL, senha));
    }

    private ErrorCode codigoDoErro(String senha) {
        try {
            entrar(senha);
            return null;
        } catch (BusinessException excecao) {
            return excecao.codigo();
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("deve zerar o contador e registrar LOGIN_SUCESSO quando a senha está certa")
        void deve_zerarContador_quando_senhaCerta() {
            usuario.setTentativasFalhas(3);

            LoginResponse resposta = entrar(SENHA);

            assertThat(resposta.token()).isEqualTo("token");
            assertThat(usuario.getTentativasFalhas()).isZero();
            verify(auditoria).registrar(eq(usuario.getId()), eq(AcaoAuditoria.LOGIN_SUCESSO), eq("usuario"),
                eq(usuario.getId()), isNull());
        }

        @Test
        @DisplayName("deve contar a falha e registrar LOGIN_FALHOU quando a senha está errada")
        void deve_contarFalha_quando_senhaErrada() {
            assertThat(codigoDoErro("errada")).isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS);

            assertThat(usuario.getTentativasFalhas()).isEqualTo(1);
            verify(auditoria).registrar(eq(usuario.getId()), eq(AcaoAuditoria.LOGIN_FALHOU), eq("usuario"),
                eq(usuario.getId()), anyMap());
        }

        @Test
        @DisplayName("deve registrar LOGIN_FALHOU sem usuário quando o e-mail não existe")
        void deve_registrarFalha_quando_emailInexistente() {
            assertThatThrownBy(() -> servico.login(new LoginRequest("ninguem@canteiro.test", SENHA)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).codigo())
                .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS);
            verify(auditoria).registrar(isNull(), eq(AcaoAuditoria.LOGIN_FALHOU), eq("usuario"), isNull(),
                anyMap());
        }

        @Test
        @DisplayName("deve bloquear por 15 minutos na quinta senha errada seguida e recusar a sexta tentativa")
        void deve_bloquear_quando_cincoSenhasErradas() {
            for (int tentativa = 1; tentativa <= 4; tentativa++) {
                assertThat(codigoDoErro("errada")).isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS);
            }
            assertThat(codigoDoErro("errada")).isEqualTo(ErrorCode.CONTA_BLOQUEADA);
            assertThat(usuario.getBloqueadoAte().toInstant()).isEqualTo(Instant.parse("2026-11-01T12:15:00Z"));
            verify(auditoria).registrar(eq(usuario.getId()), eq(AcaoAuditoria.CONTA_BLOQUEADA), eq("usuario"),
                eq(usuario.getId()), anyMap());

            assertThat(codigoDoErro(SENHA)).isEqualTo(ErrorCode.CONTA_BLOQUEADA);
            verify(jwtService, never()).gerarToken(any());
        }

        @Test
        @DisplayName("deve informar bloqueadoAte e os minutos restantes quando a conta está bloqueada")
        void deve_informarPrazo_quando_contaBloqueada() {
            for (int tentativa = 1; tentativa <= 5; tentativa++) {
                codigoDoErro("errada");
            }
            relogio.avancar(Duration.ofMinutes(10));

            assertThatThrownBy(() -> entrar(SENHA))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5 minutos")
                .satisfies(e -> assertThat(((BusinessException) e).propriedades())
                    .containsEntry("bloqueadoAte", "2026-11-01T12:15:00Z"));
        }

        @Test
        @DisplayName("deve aceitar a senha certa depois que o prazo do bloqueio passa")
        void deve_aceitar_quando_prazoDoBloqueioPassa() {
            for (int tentativa = 1; tentativa <= 5; tentativa++) {
                codigoDoErro("errada");
            }
            relogio.avancar(Duration.ofMinutes(15));

            assertThat(entrar(SENHA).token()).isEqualTo("token");
            assertThat(usuario.getBloqueadoAte()).isNull();
            verify(jwtService, times(1)).gerarToken(usuario);
        }

        @Test
        @DisplayName("deve recusar com USUARIO_INATIVO quando a senha está certa mas o usuário está desativado")
        void deve_recusar_quando_usuarioDesativado() {
            usuario.setAtivo(false);

            assertThat(codigoDoErro(SENHA)).isEqualTo(ErrorCode.USUARIO_INATIVO);
            verify(jwtService, never()).gerarToken(any());
        }
    }
}
