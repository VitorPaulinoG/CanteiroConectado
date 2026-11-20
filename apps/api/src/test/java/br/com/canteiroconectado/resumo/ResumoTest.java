package br.com.canteiroconectado.resumo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class, Requisicoes.class})
@DisplayName("GET /api/v1/canteiros/{id}/estoque/resumo")
class ResumoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private Requisicoes requisicoes;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Usuario camila;

    @BeforeEach
    void preparar() {
        jdbc.update("UPDATE item SET ativo = false WHERE ativo");
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        camila = tokens.gestor();
    }

    private Item item(String descricao, String minimo, String saldo) {
        Item item = dados.item(descricao, Unidade.UN, TipoItem.MATERIAL, minimo);
        if (saldo != null) {
            jdbc.update("INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade) VALUES (?, ?, ?)",
                aurora.getId(), item.getId(), new BigDecimal(saldo));
        }
        return item;
    }

    @Nested
    @DisplayName("alertas de estoque")
    class Alertas {

        @Test
        @DisplayName("deve contar 2 zerados e 3 abaixo do mínimo e listar os três mais críticos")
        void deve_contarEListarCriticos_quando_haAlertas() throws Exception {
            item("Brita", "10", "0");
            item("Areia", "5", null);
            item("Cimento CP-II", "50", "40");
            item("Bloco", "100", "10");
            item("Cal", "20", "15");
            item("Prego", "0", "0");
            item("Tijolo", "10", "30");

            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/resumo")
                    .header("Authorization", tokens.bearer(camila)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zerados").value(2))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(3))
                .andExpect(jsonPath("$.maisCriticos", hasSize(3)))
                .andExpect(jsonPath("$.maisCriticos[*].descricao", contains("Areia", "Brita", "Bloco")));
        }

        @Test
        @DisplayName("deve vir zerado quando não há alerta")
        void deve_virZerado_quando_semAlerta() throws Exception {
            item("Tijolo", "10", "30");

            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/resumo")
                    .header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(jsonPath("$.zerados").value(0))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(0))
                .andExpect(jsonPath("$.maisCriticos", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("contadores de requisição")
    class Contadores {

        @Test
        @DisplayName("deve mostrar ao ALMOXARIFE 2 para decidir e à GESTOR as dela por status")
        void deve_contarParaDecidirEMinhas_quando_haRequisicoes() throws Exception {
            Item cimento = item("Cimento CP-II", "0", null);
            requisicoes.pendente(camila, aurora.getId(), cimento.getId());
            requisicoes.pendente(camila, aurora.getId(), cimento.getId());
            requisicoes.gravar(camila, aurora.getId(), "APROVADA", Instant.now(), cimento.getId());
            requisicoes.pendente(sebastiao, aurora.getId(), cimento.getId());

            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/resumo")
                    .header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(jsonPath("$.requisicoesParaDecidir").value(2))
                .andExpect(jsonPath("$.minhasRequisicoes.PENDENTE").value(1));
            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/resumo")
                    .header("Authorization", tokens.bearer(camila)))
                .andExpect(jsonPath("$.requisicoesParaDecidir").value(1))
                .andExpect(jsonPath("$.minhasRequisicoes.PENDENTE").value(2))
                .andExpect(jsonPath("$.minhasRequisicoes.APROVADA").value(1))
                .andExpect(jsonPath("$.minhasRequisicoes.RECUSADA").value(0));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE de outro canteiro")
        void deve_recusar_quando_outroCanteiro() throws Exception {
            mockMvc.perform(get("/api/v1/canteiros/" + aurora.getId() + "/estoque/resumo")
                    .header("Authorization", tokens.bearer(tokens.almoxarife())))
                .andExpect(status().isForbidden());
        }
    }
}
