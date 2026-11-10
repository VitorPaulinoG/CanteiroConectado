package br.com.canteiroconectado.auditoria;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Leitura da trilha de auditoria com o nome do autor e do alvo numa única consulta.
 */
@Repository
public class AuditoriaConsultaRepository {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private static final String SELECAO = """
        SELECT l.id, l.acao, l.usuario_id, u.nome AS usuario_nome, l.entidade, l.entidade_id,
               CAST(l.detalhes AS text) AS detalhes, l.request_id, l.criado_em,
               CASE l.entidade
                   WHEN 'usuario' THEN alvo.nome
                   WHEN 'canteiro' THEN c.nome
                   WHEN 'item' THEN i.codigo || ' · ' || i.descricao
                   WHEN 'colaborador' THEN col.nome
                   WHEN 'requisicao' THEN 'REQ-' || CASE WHEN r.numero < 10000 THEN lpad(CAST(r.numero AS text), 4, '0')
                                                         ELSE CAST(r.numero AS text) END
                   WHEN 'movimentacao' THEN mi.descricao
               END AS entidade_descricao
        FROM log_auditoria l
        LEFT JOIN usuario u ON u.id = l.usuario_id
        LEFT JOIN usuario alvo ON l.entidade = 'usuario' AND alvo.id = l.entidade_id
        LEFT JOIN canteiro c ON l.entidade = 'canteiro' AND c.id = l.entidade_id
        LEFT JOIN item i ON l.entidade = 'item' AND i.id = l.entidade_id
        LEFT JOIN colaborador col ON l.entidade = 'colaborador' AND col.id = l.entidade_id
        LEFT JOIN requisicao r ON l.entidade = 'requisicao' AND r.id = l.entidade_id
        LEFT JOIN movimentacao_estoque m ON l.entidade = 'movimentacao' AND m.id = l.entidade_id
        LEFT JOIN item mi ON mi.id = m.item_id
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;

    public AuditoriaConsultaRepository(NamedParameterJdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    record Filtro(UUID usuarioId, AcaoAuditoria acao, Instant de, Instant ate) {
    }

    List<EventoDeAuditoriaResponse> listar(Filtro filtro, int limite, long deslocamento) {
        return jdbc.query(SELECAO + condicoes(filtro) + """
             ORDER BY l.criado_em DESC, l.id
            LIMIT :limite OFFSET :deslocamento
            """, parametros(filtro).addValue("limite", limite).addValue("deslocamento", deslocamento),
            (resultado, numero) -> new EventoDeAuditoriaResponse(
                resultado.getObject("id", UUID.class),
                AcaoAuditoria.valueOf(resultado.getString("acao")),
                resultado.getObject("usuario_id", UUID.class),
                resultado.getString("usuario_nome"),
                resultado.getString("entidade"),
                resultado.getObject("entidade_id", UUID.class),
                resultado.getString("entidade_descricao"),
                detalhes(resultado.getString("detalhes")),
                resultado.getString("request_id"),
                resultado.getObject("criado_em", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC)));
    }

    long contar(Filtro filtro) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM log_auditoria l" + condicoes(filtro), parametros(filtro),
            Long.class);
        return total == null ? 0 : total;
    }

    private Map<String, Object> detalhes(String texto) {
        return texto == null ? Map.of() : json.readValue(texto, MAPA);
    }

    private static String condicoes(Filtro filtro) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        if (filtro.usuarioId() != null) {
            sql.append(" AND l.usuario_id = :usuarioId");
        }
        if (filtro.acao() != null) {
            sql.append(" AND l.acao = :acao");
        }
        if (filtro.de() != null) {
            sql.append(" AND l.criado_em >= :de");
        }
        if (filtro.ate() != null) {
            sql.append(" AND l.criado_em < :ate");
        }
        return sql.toString();
    }

    private static MapSqlParameterSource parametros(Filtro filtro) {
        return new MapSqlParameterSource()
            .addValue("usuarioId", filtro.usuarioId())
            .addValue("acao", filtro.acao() == null ? null : filtro.acao().name())
            .addValue("de", filtro.de() == null ? null : OffsetDateTime.ofInstant(filtro.de(), ZoneOffset.UTC))
            .addValue("ate", filtro.ate() == null ? null : OffsetDateTime.ofInstant(filtro.ate(), ZoneOffset.UTC));
    }
}
