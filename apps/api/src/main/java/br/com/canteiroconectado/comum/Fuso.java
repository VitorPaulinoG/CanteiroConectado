package br.com.canteiroconectado.comum;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fuso dos canteiros, usado só para decidir qual é o dia de hoje em regras com data (prazo da
 * requisição, previsão de chegada). Horários continuam gravados em UTC.
 */
public final class Fuso {

    public static final ZoneId DAS_OBRAS = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
    }

    public static LocalDate hoje(Clock relogio) {
        return LocalDate.ofInstant(Instant.now(relogio), DAS_OBRAS);
    }
}
