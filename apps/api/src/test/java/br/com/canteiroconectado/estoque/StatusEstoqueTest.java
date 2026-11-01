package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("StatusEstoque")
class StatusEstoqueTest {

    @Nested
    @DisplayName("de")
    class De {

        @ParameterizedTest(name = "saldo {0} e mínimo {1} = {2}")
        @CsvSource({
            "0, 10, ZERADO",
            "0, 0.5, ZERADO",
            "3, 10, ABAIXO_DO_MINIMO",
            "9.999, 10, ABAIXO_DO_MINIMO",
            "10, 10, NORMAL",
            "15, 10, NORMAL",
            "0, 0, NORMAL",
            "4, 0, NORMAL"
        })
        @DisplayName("deve aplicar as três regras de status")
        void deve_aplicarRegras_quando_saldoEMinimo(String saldo, String minimo, StatusEstoque esperado) {
            assertThat(StatusEstoque.de(new BigDecimal(saldo), new BigDecimal(minimo))).isEqualTo(esperado);
        }
    }
}
