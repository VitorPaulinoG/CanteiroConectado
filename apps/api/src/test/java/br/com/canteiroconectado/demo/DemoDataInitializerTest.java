package br.com.canteiroconectado.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@DisplayName("DemoDataInitializer")
class DemoDataInitializerTest {

    @Nested
    @DisplayName("senha de demonstração")
    class Senha {

        @Test
        @DisplayName("deve impedir a subida nomeando DEMO_SENHA quando a variável está vazia ou fraca")
        void deve_falhar_quando_senhaAusenteOuFraca() {
            assertThatThrownBy(() -> new DemoDataInitializer(mock(CargaDeDemonstracao.class), ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_SENHA");
            assertThatThrownBy(() -> new DemoDataInitializer(mock(CargaDeDemonstracao.class), "somenteletras"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_SENHA");
        }

        @Test
        @DisplayName("deve carregar os dados com a senha informada")
        void deve_carregar_quando_senhaValida() {
            CargaDeDemonstracao carga = mock(CargaDeDemonstracao.class);

            new DemoDataInitializer(carga, "Demo2026obra").run(null);

            verify(carga).carregar("Demo2026obra");
        }
    }

    @Nested
    @SpringBootTest
    @DisplayName("sem o perfil demo")
    class SemPerfil {

        @Autowired
        private ApplicationContext contexto;

        @Test
        @DisplayName("não deve existir na subida padrão")
        void naoDeve_existir_quando_perfilDemoInativo() {
            assertThat(contexto.getBeansOfType(DemoDataInitializer.class)).isEmpty();
        }
    }
}
