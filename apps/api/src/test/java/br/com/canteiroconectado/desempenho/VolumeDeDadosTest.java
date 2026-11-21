package br.com.canteiroconectado.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

/**
 * Mede as listas de estoque, histórico e requisições com 2 mil itens, 10 mil movimentações e 500
 * requisições num canteiro. Grava dados de verdade (sem desfazer), então só roda quando pedido e
 * deve usar um banco próprio:
 *
 * <pre>
 * BANCO_URL=jdbc:postgresql://localhost:5432/canteiroconectado_volume \
 *   ./mvnw test -Dtest=VolumeDeDadosTest -Ddesempenho=true -Djacoco.skip=true
 * </pre>
 *
 * O resultado sai em {@code target/desempenho/resultado.md}: mediana e pior tempo de 20 chamadas
 * de cada tela, depois de 3 chamadas de aquecimento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "desempenho", matches = "true")
@Import({Tokens.class, Dados.class})
@DisplayName("Volume de dados")
class VolumeDeDadosTest {

    static final int ITENS = 2_000;
    static final int MOVIMENTACOES_POR_ITEM = 5;
    static final int REQUISICOES = 500;
    static final int COLABORADORES = 20;
    static final int AQUECIMENTO = 3;
    static final int CHAMADAS = 20;
    static final long LIMITE_DA_MEDIANA_MS = 300;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacoes;

    private Canteiro canteiro;
    private Usuario almoxarife;
    private Usuario gestor;
    private UUID item;
    private UUID colaborador;

    @BeforeAll
    void carregar() {
        canteiro = dados.canteiro();
        almoxarife = tokens.almoxarife(canteiro.getId());
        gestor = tokens.gestor();
        for (int i = 0; i < COLABORADORES; i++) {
            dados.colaborador(canteiro.getId());
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("c", canteiro.getId())
            .addValue("a", almoxarife.getId())
            .addValue("prefixo", Dados.unico("V") + "-")
            .addValue("itens", ITENS)
            .addValue("requisicoes", REQUISICOES);
        new TransactionTemplate(transacoes).executeWithoutResult(status -> gravar(p));
        jdbc.getJdbcTemplate().execute("ANALYZE");

        item = jdbc.queryForObject("SELECT id FROM item WHERE codigo = :prefixo || '00777'", p, UUID.class);
        colaborador = jdbc.queryForObject("SELECT id FROM colaborador WHERE canteiro_id = :c ORDER BY nome LIMIT 1",
            p, UUID.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque WHERE canteiro_id = :c", p,
            Integer.class)).isEqualTo(ITENS * MOVIMENTACOES_POR_ITEM);
    }

    private void gravar(MapSqlParameterSource p) {
        jdbc.update("""
            INSERT INTO item (id, codigo, descricao, unidade, tipo, quantidade_minima)
            SELECT gen_random_uuid(), :prefixo || lpad(n::text, 5, '0'),
                   (ARRAY['Cimento CP-II', 'Areia média', 'Brita 1', 'Tijolo cerâmico', 'Vergalhão 10mm',
                          'Tubo PVC', 'Tinta acrílica', 'Furadeira', 'Serra circular', 'Martelete'])[1 + n % 10]
                       || ' lote ' || n,
                   'UN', CASE WHEN n % 10 >= 7 THEN 'FERRAMENTA' ELSE 'MATERIAL' END,
                   CASE WHEN n % 4 = 0 THEN 100 ELSE 10 END
            FROM generate_series(1, :itens) n
            """, p);
        jdbc.update("""
            CREATE TEMPORARY TABLE volume_item ON COMMIT DROP AS
            SELECT id, CAST(substring(codigo FROM '[0-9]+$') AS int) AS n,
                   CASE WHEN CAST(substring(codigo FROM '[0-9]+$') AS int) % 50 = 0 THEN 4 ELSE 100 END AS entrada
            FROM item WHERE codigo LIKE :prefixo || '%'
            """, p);
        jdbc.update("""
            WITH colaboradores AS (SELECT array_agg(id ORDER BY nome) AS ids FROM colaborador WHERE canteiro_id = :c)
            INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos,
                                              colaborador_id, frente_obra, registrado_por, criado_em)
            SELECT gen_random_uuid(), :c, v.id,
                   CASE WHEN k = 0 THEN 'ENTRADA' ELSE 'SAIDA' END,
                   CASE WHEN k = 0 THEN 'RECEBIMENTO' ELSE 'RETIRADA' END,
                   CASE WHEN k = 0 THEN v.entrada ELSE 1 END,
                   v.entrada - k,
                   CASE WHEN k = 0 THEN NULL ELSE c.ids[1 + (v.n + k) % array_length(c.ids, 1)] END,
                   CASE WHEN k = 0 THEN NULL ELSE 'Frente ' || (v.n % 5) END,
                   :a,
                   now() - make_interval(days => 90 - v.n % 90) + make_interval(mins => v.n % 600, hours => k)
            FROM volume_item v CROSS JOIN generate_series(0, 4) k CROSS JOIN colaboradores c
            """, p);
        jdbc.update("""
            INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade)
            SELECT :c, id, entrada - 4 FROM volume_item
            """, p);
        jdbc.update("""
            INSERT INTO requisicao (id, numero, canteiro_id, solicitante_id, solicitante_perfil, frente_obra,
                                    necessario_ate, status, criado_em)
            SELECT gen_random_uuid(), r, :c, :a, 'ALMOXARIFE', 'Frente ' || (r % 5), current_date + 7,
                   (ARRAY['PENDENTE', 'APROVADA', 'RECUSADA', 'ATENDIDA', 'CANCELADA'])[1 + r % 5],
                   now() - make_interval(hours => r * 3)
            FROM generate_series(1, :requisicoes) r
            """, p);
        jdbc.update("""
            INSERT INTO item_requisicao (id, requisicao_id, item_id, quantidade, ordem)
            SELECT gen_random_uuid(), r.id, v.id, 10, o
            FROM requisicao r
            CROSS JOIN generate_series(0, 1) o
            JOIN volume_item v ON v.n = 1 + (r.numero * 2 + o) % :itens
            WHERE r.canteiro_id = :c
            """, p);
        jdbc.update("""
            INSERT INTO requisicao_evento (id, requisicao_id, status, usuario_id, criado_em)
            SELECT gen_random_uuid(), id, 'PENDENTE', :a, criado_em FROM requisicao WHERE canteiro_id = :c
            """, p);
        jdbc.update("INSERT INTO requisicao_sequencia (canteiro_id, ultimo) VALUES (:c, :requisicoes)", p);
    }

    record Medida(String tela, long medianaMs, long piorMs) {
    }

    private Medida medir(String tela, Usuario usuario, MockHttpServletRequestBuilder pedido) throws Exception {
        String token = tokens.bearer(usuario);
        for (int i = 0; i < AQUECIMENTO; i++) {
            mockMvc.perform(pedido.header("Authorization", token)).andExpect(status().isOk());
        }
        long[] tempos = new long[CHAMADAS];
        for (int i = 0; i < CHAMADAS; i++) {
            long inicio = System.nanoTime();
            mockMvc.perform(pedido.header("Authorization", token)).andExpect(status().isOk());
            tempos[i] = (System.nanoTime() - inicio) / 1_000_000;
        }
        Arrays.sort(tempos);
        return new Medida(tela, tempos[CHAMADAS / 2], tempos[CHAMADAS - 1]);
    }

    @Test
    @DisplayName("deve responder as listas principais com mediana abaixo do limite")
    void deve_responderRapido_quando_canteiroTemMuitosDados() throws Exception {
        String c = canteiro.getId().toString();
        String hoje = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString();
        String semanaPassada = LocalDate.now(ZoneOffset.UTC).minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC)
            .toString();
        Map<String, MockHttpServletRequestBuilder> telas = new LinkedHashMap<>();
        Map<String, Usuario> quem = new LinkedHashMap<>();
        telas.put("S06 estoque, 1ª página", get("/api/v1/canteiros/" + c + "/estoque"));
        telas.put("S06 estoque, busca \"cimento\"", get("/api/v1/canteiros/" + c + "/estoque").param("q", "cimento"));
        telas.put("S06 estoque, só alertas", get("/api/v1/canteiros/" + c + "/estoque")
            .param("status", "ZERADO", "ABAIXO_DO_MINIMO"));
        telas.put("S06 estoque, página 50", get("/api/v1/canteiros/" + c + "/estoque").param("pagina", "50"));
        telas.put("S07 detalhe do item", get("/api/v1/canteiros/" + c + "/estoque/" + item));
        telas.put("S04 resumo do Início", get("/api/v1/canteiros/" + c + "/estoque/resumo"));
        telas.put("S16 histórico, 1ª página", get("/api/v1/movimentacoes").param("canteiroId", c));
        telas.put("S16 histórico, últimos 7 dias", get("/api/v1/movimentacoes").param("canteiroId", c)
            .param("de", semanaPassada).param("ate", hoje));
        telas.put("S16 histórico de um item", get("/api/v1/movimentacoes").param("canteiroId", c)
            .param("itemId", item.toString()));
        telas.put("S16 histórico de um colaborador", get("/api/v1/movimentacoes").param("canteiroId", c)
            .param("colaboradorId", colaborador.toString()));
        telas.put("S16 histórico, página 400", get("/api/v1/movimentacoes").param("canteiroId", c)
            .param("pagina", "400"));
        telas.put("S12 requisições, todas", get("/api/v1/requisicoes").param("canteiroId", c));
        telas.put("S12 requisições, para decidir", get("/api/v1/requisicoes").param("canteiroId", c)
            .param("escopo", "para-decidir"));
        telas.keySet().forEach(tela -> quem.put(tela, tela.startsWith("S06") || tela.startsWith("S07")
            ? almoxarife : gestor));

        List<Medida> medidas = new ArrayList<>();
        for (Map.Entry<String, MockHttpServletRequestBuilder> tela : telas.entrySet()) {
            medidas.add(medir(tela.getKey(), quem.get(tela.getKey()), tela.getValue()));
        }
        relatar(medidas);

        assertThat(medidas).allSatisfy(medida -> assertThat(medida.medianaMs())
            .as(medida.tela()).isLessThan(LIMITE_DA_MEDIANA_MS));
    }

    private void relatar(List<Medida> medidas) throws IOException {
        StringBuilder relatorio = new StringBuilder("""
            | Tela | Mediana (ms) | Pior (ms) |
            | --- | --- | --- |
            """);
        medidas.forEach(medida -> relatorio.append("| %s | %d | %d |%n".formatted(medida.tela(), medida.medianaMs(),
            medida.piorMs())));
        Path arquivo = Path.of("target", "desempenho", "resultado.md");
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, relatorio);
    }
}
