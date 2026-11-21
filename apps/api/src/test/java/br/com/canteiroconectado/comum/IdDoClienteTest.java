package br.com.canteiroconectado.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

@DisplayName("IdDoCliente")
class IdDoClienteTest {

    @Nested
    @DisplayName("ler")
    class Ler {

        @Test
        @DisplayName("deve aceitar UUID canônico, com maiúsculas e espaços nas pontas")
        void deve_aceitar_quando_uuidCanonico() {
            UUID id = UUID.randomUUID();

            assertThat(IdDoCliente.ler(id.toString())).isEqualTo(id);
            assertThat(IdDoCliente.ler(" " + id.toString().toUpperCase() + " ")).isEqualTo(id);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "123", "1-1-1-1-1", "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz",
            "0e8c4b3a-5f3e-4c1a-9b0a-3c6f8f2a1b2"})
        @DisplayName("deve recusar com ID_INVALIDO vazio ou fora do formato 8-4-4-4-12")
        void deve_recusar_quando_naoEhUuid(String texto) {
            assertThatThrownBy(() -> IdDoCliente.ler(texto))
                .isInstanceOf(BusinessException.class)
                .satisfies(erro -> {
                    assertThat(((BusinessException) erro).codigo()).isEqualTo(ErrorCode.ID_INVALIDO);
                    assertThat(((BusinessException) erro).propriedades()).containsEntry("campo", "id");
                });
        }
    }
}
