package br.com.canteiroconectado.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.config.AdminProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer")
class AdminInitializerTest {

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private PasswordEncoder encoderSenha;

    @Mock
    private AuditoriaService auditoria;

    private AdminInitializer inicializador;

    @BeforeEach
    void montar() {
        AdminProperties propriedades = new AdminProperties(" Admin ", " Admin@Canteiro.Test ", "senha-forte-123");
        inicializador = new AdminInitializer(usuarios, encoderSenha, auditoria, propriedades);
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("deve criar o GESTOR administrador com troca de senha pendente quando não há administrador")
        void deve_criarAdministrador_quando_naoHaAdministrador() {
            when(usuarios.existsByAdministradorTrue()).thenReturn(false);
            when(usuarios.existsByEmailIgnoreCase("admin@canteiro.test")).thenReturn(false);
            when(encoderSenha.encode("senha-forte-123")).thenReturn("hash");

            inicializador.run(null);

            ArgumentCaptor<Usuario> criado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarios).save(criado.capture());
            Usuario admin = criado.getValue();
            assertThat(admin.getNome()).isEqualTo("Admin");
            assertThat(admin.getEmail()).isEqualTo("admin@canteiro.test");
            assertThat(admin.getSenhaHash()).isEqualTo("hash");
            assertThat(admin.getPerfil()).isEqualTo(Perfil.GESTOR);
            assertThat(admin.isAdministrador()).isTrue();
            assertThat(admin.getCanteiroId()).isNull();
            assertThat(admin.isPrecisaTrocarSenha()).isTrue();
            verify(auditoria).registrar(isNull(), eq(AcaoAuditoria.ADMIN_CRIADO), eq("usuario"),
                eq(admin.getId()), anyMap());
        }

        @Test
        @DisplayName("não deve fazer nada quando já existe administrador")
        void naoDeve_fazerNada_quando_jaExisteAdministrador() {
            when(usuarios.existsByAdministradorTrue()).thenReturn(true);

            inicializador.run(null);

            verify(usuarios, never()).save(any());
            verifyNoInteractions(auditoria, encoderSenha);
        }

        @Test
        @DisplayName("deve falhar com mensagem clara quando o e-mail já pertence a outro usuário")
        void deve_falhar_quando_emailJaUsado() {
            when(usuarios.existsByAdministradorTrue()).thenReturn(false);
            when(usuarios.existsByEmailIgnoreCase("admin@canteiro.test")).thenReturn(true);

            assertThatThrownBy(() -> inicializador.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
            verify(usuarios, never()).save(any());
        }
    }
}
