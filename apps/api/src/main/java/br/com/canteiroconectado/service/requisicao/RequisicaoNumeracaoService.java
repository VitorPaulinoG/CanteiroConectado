package br.com.canteiroconectado.service.requisicao;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RequisicaoNumeracaoService {

    private final JdbcTemplate jdbc;

    RequisicaoNumeracaoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    int proximo(UUID canteiroId) {
        Integer numero = jdbc.queryForObject("""
            INSERT INTO requisicao_sequencia (canteiro_id, ultimo) VALUES (?, 1)
            ON CONFLICT (canteiro_id) DO UPDATE SET ultimo = requisicao_sequencia.ultimo + 1
            RETURNING ultimo
            """, Integer.class, canteiroId);
        if (numero == null) {
            throw new IllegalStateException("Numeração de requisição não devolveu valor.");
        }
        return numero;
    }

    public static String formatar(int numero) {
        return "REQ-%04d".formatted(numero);
    }
}
