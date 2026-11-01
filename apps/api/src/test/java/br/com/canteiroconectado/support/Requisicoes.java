package br.com.canteiroconectado.support;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.canteiroconectado.usuario.Usuario;

/**
 * Grava requisições direto no banco de teste, com o status e a data desejados, para testar
 * consultas e transições sem depender do fluxo inteiro.
 */
@Component
public class Requisicoes {

    private final JdbcTemplate jdbc;

    public Requisicoes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID gravar(Usuario solicitante, UUID canteiroId, String status, Instant criadoEm, UUID itemId) {
        UUID id = UUID.randomUUID();
        Integer numero = jdbc.queryForObject("""
            INSERT INTO requisicao_sequencia (canteiro_id, ultimo) VALUES (?, 1)
            ON CONFLICT (canteiro_id) DO UPDATE SET ultimo = requisicao_sequencia.ultimo + 1
            RETURNING ultimo
            """, Integer.class, canteiroId);
        jdbc.update("""
            INSERT INTO requisicao (id, numero, canteiro_id, solicitante_id, solicitante_perfil, necessario_ate, status,
                                    criado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, id, numero, canteiroId, solicitante.getId(), solicitante.getPerfil().name(),
            LocalDate.now().plusDays(7), status, Timestamp.from(criadoEm));
        jdbc.update("INSERT INTO item_requisicao (id, requisicao_id, item_id, quantidade, ordem) VALUES (?, ?, ?, ?, 0)",
            UUID.randomUUID(), id, itemId, new BigDecimal("10"));
        jdbc.update("""
            INSERT INTO requisicao_evento (id, requisicao_id, status, usuario_id, criado_em) VALUES (?, ?, 'PENDENTE', ?, ?)
            """, UUID.randomUUID(), id, solicitante.getId(), Timestamp.from(criadoEm));
        return id;
    }

    public UUID pendente(Usuario solicitante, UUID canteiroId, UUID itemId) {
        return gravar(solicitante, canteiroId, "PENDENTE", Instant.now(), itemId);
    }
}
