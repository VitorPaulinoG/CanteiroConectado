package br.com.canteiroconectado.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CargaDeDemonstracao")
class CargaDeDemonstracaoTest {

    static final String SENHA = "Demo2026obra";

    @Autowired
    private CargaDeDemonstracao carga;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private String entrar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.precisaTrocarSenha").value(false))
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private UUID aurora() {
        return jdbc.queryForObject("SELECT id FROM canteiro WHERE nome = ?", UUID.class,
            CargaDeDemonstracao.CANTEIRO_PRINCIPAL);
    }

    @Nested
    @DisplayName("carregar")
    class Carregar {

        @Test
        @DisplayName("deve criar os canteiros, cerca de 20 itens e os usuários que entram com a senha de demonstração")
        void deve_criarCenario_quando_bancoSemDemonstracao() throws Exception {
            assertThat(carga.carregar(SENHA)).isTrue();
            entityManager.flush();

            UUID aurora = aurora();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM canteiro WHERE nome IN (?, ?)", Integer.class,
                CargaDeDemonstracao.CANTEIRO_PRINCIPAL, CargaDeDemonstracao.CANTEIRO_SECUNDARIO)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM item WHERE codigo LIKE 'MAT-%' OR codigo LIKE 'FER-%'",
                Integer.class)).isEqualTo(22);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM colaborador WHERE canteiro_id = ?", Integer.class,
                aurora)).isEqualTo(4);

            String sebastiao = entrar(CargaDeDemonstracao.EMAIL_ALMOXARIFE);
            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", sebastiao))
                .andExpect(jsonPath("$.perfil").value("ALMOXARIFE"))
                .andExpect(jsonPath("$.canteiroId").value(aurora.toString()));
            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization",
                    entrar(CargaDeDemonstracao.EMAIL_GESTORA)))
                .andExpect(jsonPath("$.perfil").value("GESTOR"));
        }

        @Test
        @DisplayName("deve deixar 2 furadeiras disponíveis quando o Sebastião busca furadeira")
        void deve_mostrarDuasFuradeiras_quando_sebastiaoBusca() throws Exception {
            carga.carregar(SENHA);

            mockMvc.perform(get("/api/v1/canteiros/" + aurora() + "/estoque").param("q", "furadeira")
                    .header("Authorization", entrar(CargaDeDemonstracao.EMAIL_ALMOXARIFE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].descricao").value("Furadeira de impacto 700W"))
                .andExpect(jsonPath("$.conteudo[0].saldo").value(2));
        }

        @Test
        @DisplayName("deve fazer os saldos nascerem só de movimentações, com entradas, saídas, ajuste e estorno")
        void deve_derivarSaldosDasMovimentacoes_quando_carrega() {
            carga.carregar(SENHA);
            entityManager.flush();
            UUID aurora = aurora();

            List<Map<String, Object>> divergentes = jdbc.queryForList("""
                SELECT s.item_id FROM saldo_estoque s
                WHERE s.canteiro_id = ?
                  AND s.quantidade <> (SELECT COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade
                                                                ELSE -m.quantidade END), 0)
                                       FROM movimentacao_estoque m
                                       WHERE m.canteiro_id = s.canteiro_id AND m.item_id = s.item_id)
                """, aurora);
            assertThat(divergentes).isEmpty();
            assertThat(jdbc.queryForList("SELECT DISTINCT motivo FROM movimentacao_estoque WHERE canteiro_id = ?",
                String.class, aurora)).contains("RECEBIMENTO", "RETIRADA", "AJUSTE", "ESTORNO");
        }

        @Test
        @DisplayName("deve deixar uma requisição pendente do Sebastião para a Camila decidir")
        void deve_deixarRequisicaoParaDecidir_quando_carrega() throws Exception {
            carga.carregar(SENHA);

            mockMvc.perform(get("/api/v1/requisicoes").param("canteiroId", aurora().toString())
                    .param("escopo", "para-decidir")
                    .header("Authorization", entrar(CargaDeDemonstracao.EMAIL_GESTORA)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.conteudo[0].solicitanteNome").value("Sebastião Ferreira"))
                .andExpect(jsonPath("$.conteudo[0].itens[*].descricao", hasItem("Bloco de concreto 14x19x39")));
        }

        @Test
        @DisplayName("não deve carregar de novo quando os dados já existem")
        void naoDeve_carregarDeNovo_quando_jaCarregado() {
            assertThat(carga.carregar(SENHA)).isTrue();
            entityManager.flush();
            Integer movimentacoes = jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque", Integer.class);

            assertThat(carga.carregar(SENHA)).isFalse();

            assertThat(jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque", Integer.class))
                .isEqualTo(movimentacoes);
        }
    }
}
