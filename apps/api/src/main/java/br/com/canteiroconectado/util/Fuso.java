package br.com.canteiroconectado.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Fuso {

    public static final ZoneId DAS_OBRAS = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
    }

    public static LocalDate hoje(Clock relogio) {
        return LocalDate.ofInstant(Instant.now(relogio), DAS_OBRAS);
    }
}
