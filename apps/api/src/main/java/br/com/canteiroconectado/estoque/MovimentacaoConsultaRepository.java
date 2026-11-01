package br.com.canteiroconectado.estoque;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.item.Unidade;

/**
 * Leitura das movimentações já com os nomes relacionados, numa única consulta (sem N+1).
 */
@Repository
public class MovimentacaoConsultaRepository {

    static final String SELECAO = """
        SELECT m.id, m.canteiro_id, c.nome AS canteiro_nome, m.item_id, i.codigo AS item_codigo,
               i.descricao AS item_descricao, i.unidade, m.tipo, m.motivo, m.quantidade, m.saldo_apos,
               m.colaborador_id, col.nome AS colaborador_nome, m.frente_obra, m.observacao, m.estorna_id,
               est.id AS estornada_por_id, m.registrado_por, u.nome AS registrado_por_nome, m.criado_em
        FROM movimentacao_estoque m
        JOIN canteiro c ON c.id = m.canteiro_id
        JOIN item i ON i.id = m.item_id
        JOIN usuario u ON u.id = m.registrado_por
        LEFT JOIN colaborador col ON col.id = m.colaborador_id
        LEFT JOIN movimentacao_estoque est ON est.estorna_id = m.id
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public MovimentacaoConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<MovimentacaoResponse> buscar(UUID id) {
        List<MovimentacaoResponse> encontradas = jdbc.query(SELECAO + " WHERE m.id = :id",
            new MapSqlParameterSource("id", id), MovimentacaoConsultaRepository::linha);
        return encontradas.stream().findFirst();
    }

    static MovimentacaoResponse linha(ResultSet resultado, int numero) throws SQLException {
        return new MovimentacaoResponse(
            resultado.getObject("id", UUID.class),
            resultado.getObject("canteiro_id", UUID.class),
            resultado.getString("canteiro_nome"),
            resultado.getObject("item_id", UUID.class),
            resultado.getString("item_codigo"),
            resultado.getString("item_descricao"),
            Unidade.valueOf(resultado.getString("unidade")).getSimbolo(),
            TipoMovimentacao.valueOf(resultado.getString("tipo")),
            MotivoMovimentacao.valueOf(resultado.getString("motivo")),
            Quantidades.normalizar(resultado.getBigDecimal("quantidade")),
            Quantidades.normalizar(resultado.getBigDecimal("saldo_apos")),
            resultado.getObject("colaborador_id", UUID.class),
            resultado.getString("colaborador_nome"),
            resultado.getString("frente_obra"),
            resultado.getString("observacao"),
            resultado.getObject("estorna_id", UUID.class),
            resultado.getObject("estornada_por_id", UUID.class),
            resultado.getObject("registrado_por", UUID.class),
            resultado.getString("registrado_por_nome"),
            resultado.getObject("criado_em", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
