package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;

/**
 * Consultas de leitura que juntam o catálogo ativo ao saldo do canteiro numa única ida ao banco.
 * O status e a ordem (críticos primeiro, depois por descrição) são calculados no SQL, para a
 * paginação valer sobre a lista já ordenada.
 */
@Repository
public class EstoqueConsultaRepository {

    static final String ESTOQUE_CLASSIFICADO = """
        WITH estoque AS (
            SELECT i.id, i.codigo, i.descricao, i.unidade, i.tipo, i.quantidade_minima,
                   COALESCE(s.quantidade, 0) AS saldo
            FROM item i
            LEFT JOIN saldo_estoque s ON s.item_id = i.id AND s.canteiro_id = :canteiroId
            WHERE i.ativo
              AND (CAST(:tipo AS text) IS NULL OR i.tipo = CAST(:tipo AS text))
              AND (CAST(:termo AS text) IS NULL
                   OR lower(i.codigo) LIKE '%' || CAST(:termo AS text) || '%' ESCAPE '\\'
                   OR lower(i.descricao) LIKE '%' || CAST(:termo AS text) || '%' ESCAPE '\\')
        ),
        classificado AS (
            SELECT e.*,
                   CASE
                       WHEN e.quantidade_minima > 0 AND e.saldo = 0 THEN 'ZERADO'
                       WHEN e.saldo > 0 AND e.saldo < e.quantidade_minima THEN 'ABAIXO_DO_MINIMO'
                       ELSE 'NORMAL'
                   END AS status
            FROM estoque e
        )
        """;

    private static final String ORDEM_DO_STATUS =
        "CASE status WHEN 'ZERADO' THEN 0 WHEN 'ABAIXO_DO_MINIMO' THEN 1 ELSE 2 END";

    private final NamedParameterJdbcTemplate jdbc;

    public EstoqueConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ItemEstoqueResponse> listar(UUID canteiroId, String termo, TipoItem tipo,
            Collection<StatusEstoque> status, int limite, long deslocamento) {
        MapSqlParameterSource parametros = parametros(canteiroId, termo, tipo, status)
            .addValue("limite", limite)
            .addValue("deslocamento", deslocamento);
        return jdbc.query(ESTOQUE_CLASSIFICADO + """
            SELECT * FROM classificado
            WHERE status IN (:status)
            ORDER BY %s, lower(descricao), lower(codigo)
            LIMIT :limite OFFSET :deslocamento
            """.formatted(ORDEM_DO_STATUS), parametros, EstoqueConsultaRepository::linha);
    }

    public long contar(UUID canteiroId, String termo, TipoItem tipo, Collection<StatusEstoque> status) {
        Long total = jdbc.queryForObject(ESTOQUE_CLASSIFICADO + "SELECT count(*) FROM classificado WHERE status IN (:status)",
            parametros(canteiroId, termo, tipo, status), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Quantos itens ativos do canteiro estão em cada status.
     */
    public Map<StatusEstoque, Long> contarPorStatus(UUID canteiroId) {
        Map<StatusEstoque, Long> contagem = new EnumMap<>(StatusEstoque.class);
        for (StatusEstoque status : StatusEstoque.values()) {
            contagem.put(status, 0L);
        }
        jdbc.query(ESTOQUE_CLASSIFICADO + "SELECT status, count(*) AS total FROM classificado GROUP BY status",
            parametros(canteiroId, null, null, EnumSet.allOf(StatusEstoque.class)),
            resultado -> {
                contagem.put(StatusEstoque.valueOf(resultado.getString("status")), resultado.getLong("total"));
            });
        return contagem;
    }

    /**
     * Itens em alerta do mais crítico para o menos: zerados primeiro, depois os abaixo do mínimo
     * pela menor proporção entre saldo e mínimo.
     */
    public List<ItemEstoqueResponse> maisCriticos(UUID canteiroId, int limite) {
        return jdbc.query(ESTOQUE_CLASSIFICADO + """
            SELECT * FROM classificado
            WHERE status IN ('ZERADO', 'ABAIXO_DO_MINIMO')
            ORDER BY %s, saldo / quantidade_minima, lower(descricao)
            LIMIT :limite
            """.formatted(ORDEM_DO_STATUS),
            parametros(canteiroId, null, null, EnumSet.allOf(StatusEstoque.class)).addValue("limite", limite),
            EstoqueConsultaRepository::linha);
    }

    static MapSqlParameterSource parametros(UUID canteiroId, String termo, TipoItem tipo,
            Collection<StatusEstoque> status) {
        return new MapSqlParameterSource()
            .addValue("canteiroId", canteiroId)
            .addValue("termo", termo)
            .addValue("tipo", tipo == null ? null : tipo.name())
            .addValue("status", status.stream().map(Enum::name).toList());
    }

    static ItemEstoqueResponse linha(ResultSet resultado, int numero) throws SQLException {
        Unidade unidade = Unidade.valueOf(resultado.getString("unidade"));
        BigDecimal minimo = resultado.getBigDecimal("quantidade_minima");
        BigDecimal saldo = resultado.getBigDecimal("saldo");
        return new ItemEstoqueResponse(
            resultado.getObject("id", UUID.class),
            resultado.getString("codigo"),
            resultado.getString("descricao"),
            unidade,
            unidade.getSimbolo(),
            TipoItem.valueOf(resultado.getString("tipo")),
            Quantidades.normalizar(minimo),
            Quantidades.normalizar(saldo),
            StatusEstoque.valueOf(resultado.getString("status")));
    }
}
