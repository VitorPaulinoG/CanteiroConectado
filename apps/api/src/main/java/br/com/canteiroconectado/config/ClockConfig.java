package br.com.canteiroconectado.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da API, sempre em UTC. Os services recebem o {@link Clock} por injeção para que
 * regras com prazo (bloqueio de login, datas de requisição) possam ser testadas com hora fixa.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
