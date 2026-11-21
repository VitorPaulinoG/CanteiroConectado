package br.com.canteiroconectado.demo;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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

import br.com.canteiroconectado.comum.Fuso;

/**
 * Jornada 2 com os dados de demonstração, pela API e na ordem em que a Camila usa o app: entra,
 * escolhe o canteiro, vê o alerta no Início, abre o estoque filtrado, abre o cimento, pede o
 * material e acompanha a resposta do Sebastião.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Jornada 2 até o alerta e a resposta")
class Jornada2Test {

    @Autowired
    private CargaDeDemonstracao carga;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String camila;
    private String sebastiao;
    private UUID aurora;
    private UUID cimento;

    private String entrar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + CargaDeDemonstracaoTest.SENHA + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    @BeforeEach
    void carregarDemonstracao() throws Exception {
        carga.carregar(CargaDeDemonstracaoTest.SENHA);
        camila = entrar(CargaDeDemonstracao.EMAIL_GESTORA);
        sebastiao = entrar(CargaDeDemonstracao.EMAIL_ALMOXARIFE);
        aurora = jdbc.queryForObject("SELECT id FROM canteiro WHERE nome = ?", UUID.class,
            CargaDeDemonstracao.CANTEIRO_PRINCIPAL);
        cimento = jdbc.queryForObject("SELECT id FROM item WHERE codigo = 'MAT-0001'", UUID.class);
    }

    @Nested
    @DisplayName("Camila")
    class Camila {

        @Test
        @DisplayName("deve ver o Cimento CP-II abaixo do mínimo no Início, no estoque filtrado e no detalhe")
        void deve_verCimentoAbaixoDoMinimo_quando_abreOApp() throws Exception {
            mockMvc.perform(get("/api/v1/canteiros").header("Authorization", camila))
                .andExpect(jsonPath("$[*].nome", hasItem(CargaDeDemonstracao.CANTEIRO_PRINCIPAL)));

            mockMvc.perform(get("/api/v1/canteiros/" + aurora + "/estoque/resumo").header("Authorization", camila))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(2))
                .andExpect(jsonPath("$.maisCriticos[*].descricao", hasItem("Argamassa AC-III 20kg")));

            mockMvc.perform(get("/api/v1/canteiros/" + aurora + "/estoque")
                    .param("status", "ZERADO", "ABAIXO_DO_MINIMO")
                    .header("Authorization", camila))
                .andExpect(jsonPath("$.conteudo[*].descricao", hasItem("Cimento CP-II 50kg")));

            mockMvc.perform(get("/api/v1/canteiros/" + aurora + "/estoque/" + cimento).header("Authorization", camila))
                .andExpect(jsonPath("$.status").value("ABAIXO_DO_MINIMO"))
                .andExpect(jsonPath("$.saldo").value(30))
                .andExpect(jsonPath("$.quantidadeMinima").value(50))
                .andExpect(jsonPath("$.unidadeSimbolo").value("saco"));
        }

        @Test
        @DisplayName("deve pedir 40 sacos, ver a requisição pendente e depois a previsão de chegada dada pelo Sebastião")
        void deve_acompanharRequisicao_quando_pedeCimento() throws Exception {
            LocalDate previsao = LocalDate.now(Fuso.DAS_OBRAS).plusDays(3);
            String criada = mockMvc.perform(post("/api/v1/requisicoes")
                    .header("Authorization", camila)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":\"" + UUID.randomUUID() + "\",\"canteiroId\":\"" + aurora + "\",\"necessarioAte\":\""
                        + LocalDate.now(Fuso.DAS_OBRAS).plusDays(5) + "\",\"frenteObra\":\"Laje do 3º andar\","
                        + "\"itens\":[{\"itemId\":\"" + cimento + "\",\"quantidade\":40}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(criada, "$.id");

            mockMvc.perform(get("/api/v1/requisicoes").param("canteiroId", aurora.toString()).param("escopo", "minhas")
                    .header("Authorization", camila))
                .andExpect(jsonPath("$.conteudo[0].id").value(id));
            mockMvc.perform(get("/api/v1/canteiros/" + aurora + "/estoque/resumo").header("Authorization", sebastiao))
                .andExpect(jsonPath("$.requisicoesParaDecidir").value(1));

            mockMvc.perform(post("/api/v1/requisicoes/" + id + "/aprovar")
                    .header("Authorization", sebastiao)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"previsaoChegada\":\"" + previsao + "\"}"))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/requisicoes/" + id).header("Authorization", camila))
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.previsaoChegada").value(previsao.toString()))
                .andExpect(jsonPath("$.decididoPorNome").value("Sebastião Ferreira"));
        }
    }
}
