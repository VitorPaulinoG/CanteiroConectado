package br.com.canteiroconectado.requisicao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.usuario.Perfil;

/**
 * Leitura das requisições com os nomes relacionados. Os itens de várias requisições vêm numa
 * única consulta extra, sem N+1.
 */
@Repository
public class RequisicaoConsultaRepository {

    static final String SELECAO = """
        SELECT r.id, r.numero, r.canteiro_id, c.nome AS canteiro_nome, r.solicitante_id,
               s.nome AS solicitante_nome, r.solicitante_perfil, r.frente_obra, r.observacao, r.necessario_ate,
               r.status, r.decidido_por, d.nome AS decidido_por_nome, r.decidido_em, r.observacao_decisao,
               r.previsao_chegada, r.criado_em
        FROM requisicao r
        JOIN canteiro c ON c.id = r.canteiro_id
        JOIN usuario s ON s.id = r.solicitante_id
        LEFT JOIN usuario d ON d.id = r.decidido_por
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public RequisicaoConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RequisicaoResponse> buscar(UUID id) {
        List<EventoDaRequisicaoResponse> historico = historico(id);
        List<ItemDaRequisicaoResponse> itens = itens(List.of(id)).getOrDefault(id, List.of());
        return jdbc.query(SELECAO + " WHERE r.id = :id", new MapSqlParameterSource("id", id),
            (resultado, numero) -> linha(resultado, itens, historico)).stream().findFirst();
    }

    Map<UUID, List<ItemDaRequisicaoResponse>> itens(Collection<UUID> requisicoes) {
        Map<UUID, List<ItemDaRequisicaoResponse>> porRequisicao = new LinkedHashMap<>();
        if (requisicoes.isEmpty()) {
            return porRequisicao;
        }
        jdbc.query("""
            SELECT ir.requisicao_id, ir.item_id, i.codigo, i.descricao, i.unidade, ir.quantidade
            FROM item_requisicao ir
            JOIN item i ON i.id = ir.item_id
            WHERE ir.requisicao_id IN (:ids)
            ORDER BY ir.requisicao_id, ir.ordem
            """, new MapSqlParameterSource("ids", requisicoes), resultado -> {
                porRequisicao.computeIfAbsent(resultado.getObject("requisicao_id", UUID.class), id -> new ArrayList<>())
                    .add(new ItemDaRequisicaoResponse(
                        resultado.getObject("item_id", UUID.class),
                        resultado.getString("codigo"),
                        resultado.getString("descricao"),
                        Unidade.valueOf(resultado.getString("unidade")).getSimbolo(),
                        Quantidades.normalizar(resultado.getBigDecimal("quantidade"))));
            });
        return porRequisicao;
    }

    List<EventoDaRequisicaoResponse> historico(UUID requisicaoId) {
        return jdbc.query("""
            SELECT e.status, e.usuario_id, u.nome, e.observacao, e.criado_em
            FROM requisicao_evento e
            JOIN usuario u ON u.id = e.usuario_id
            WHERE e.requisicao_id = :id
            ORDER BY e.criado_em, e.id
            """, new MapSqlParameterSource("id", requisicaoId), (resultado, numero) -> new EventoDaRequisicaoResponse(
                StatusRequisicao.valueOf(resultado.getString("status")),
                resultado.getObject("usuario_id", UUID.class),
                resultado.getString("nome"),
                resultado.getString("observacao"),
                utc(resultado, "criado_em")));
    }

    static RequisicaoResponse linha(ResultSet resultado, List<ItemDaRequisicaoResponse> itens,
            List<EventoDaRequisicaoResponse> historico) throws SQLException {
        return new RequisicaoResponse(
            resultado.getObject("id", UUID.class),
            NumeracaoDeRequisicao.formatar(resultado.getInt("numero")),
            resultado.getObject("canteiro_id", UUID.class),
            resultado.getString("canteiro_nome"),
            resultado.getObject("solicitante_id", UUID.class),
            resultado.getString("solicitante_nome"),
            Perfil.valueOf(resultado.getString("solicitante_perfil")),
            resultado.getString("frente_obra"),
            resultado.getString("observacao"),
            resultado.getObject("necessario_ate", LocalDate.class),
            StatusRequisicao.valueOf(resultado.getString("status")),
            resultado.getObject("decidido_por", UUID.class),
            resultado.getString("decidido_por_nome"),
            utc(resultado, "decidido_em"),
            resultado.getString("observacao_decisao"),
            resultado.getObject("previsao_chegada", LocalDate.class),
            utc(resultado, "criado_em"),
            itens,
            historico,
            null);
    }

    static OffsetDateTime utc(ResultSet resultado, String coluna) throws SQLException {
        OffsetDateTime valor = resultado.getObject(coluna, OffsetDateTime.class);
        return valor == null ? null : valor.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
