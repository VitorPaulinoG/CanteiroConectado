package br.com.canteiroconectado.estoque;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("EstoqueController")
class EstoqueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro aurora;
    private Usuario almoxarife;
    private String marca;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        almoxarife = tokens.almoxarife(aurora.getId());
        marca = Dados.unico("Estq").toLowerCase();
    }

    private Item item(String nome, TipoItem tipo, String minimo, String saldo) {
        Item item = dados.item(nome + " " + marca, Unidade.UN, tipo, minimo);
        if (saldo != null) {
            jdbc.update("INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade) VALUES (?, ?, ?)",
                aurora.getId(), item.getId(), new BigDecimal(saldo));
        }
        return item;
    }

    private MockHttpServletRequestBuilder estoque(UUID canteiroId) {
        return get("/api/v1/canteiros/" + canteiroId + "/estoque").param("q", marca);
    }

    @Nested
    @DisplayName("GET /api/v1/canteiros/{id}/estoque")
    class Listar {

        @Test
        @DisplayName("deve mostrar os críticos primeiro, por descrição, e o item sem movimentação com saldo 0")
        void deve_ordenarCriticosPrimeiro_quando_semFiltro() throws Exception {
            item("Areia", TipoItem.MATERIAL, "0", null);
            item("Cimento CP-II", TipoItem.MATERIAL, "50", "30");
            item("Brita", TipoItem.MATERIAL, "10", "0");
            item("Furadeira", TipoItem.FERRAMENTA, "1", "2");
            item("Bloco", TipoItem.MATERIAL, "100", null);

            mockMvc.perform(estoque(aurora.getId()).header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.conteudo[*].status",
                    contains("ZERADO", "ZERADO", "ABAIXO_DO_MINIMO", "NORMAL", "NORMAL")))
                .andExpect(jsonPath("$.conteudo[*].descricao", contains("Bloco " + marca, "Brita " + marca,
                    "Cimento CP-II " + marca, "Areia " + marca, "Furadeira " + marca)))
                .andExpect(jsonPath("$.conteudo[3].saldo").value(0))
                .andExpect(jsonPath("$.conteudo[2].saldo").value(30))
                .andExpect(jsonPath("$.conteudo[2].quantidadeMinima").value(50))
                .andExpect(jsonPath("$.conteudo[2].unidadeSimbolo").value("un"));
        }

        @Test
        @DisplayName("deve filtrar por status combinados e por tipo")
        void deve_filtrar_quando_statusETipo() throws Exception {
            item("Cimento CP-II", TipoItem.MATERIAL, "50", "30");
            item("Brita", TipoItem.MATERIAL, "10", "0");
            item("Furadeira", TipoItem.FERRAMENTA, "1", "0");
            item("Areia", TipoItem.MATERIAL, "0", "5");

            mockMvc.perform(estoque(aurora.getId()).param("status", "ZERADO", "ABAIXO_DO_MINIMO")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(jsonPath("$.conteudo[*].descricao",
                    contains("Brita " + marca, "Furadeira " + marca, "Cimento CP-II " + marca)));
            mockMvc.perform(estoque(aurora.getId()).param("status", "ZERADO").param("tipo", "FERRAMENTA")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(jsonPath("$.conteudo[*].descricao", contains("Furadeira " + marca)));
        }

        @Test
        @DisplayName("deve esconder itens inativos e usar o saldo do canteiro pedido")
        void deve_esconderInativos_quando_itemDesativado() throws Exception {
            Item inativo = item("Martelo", TipoItem.FERRAMENTA, "0", "3");
            inativo.setAtivo(false);
            item("Serrote", TipoItem.FERRAMENTA, "0", "4");
            Canteiro outro = dados.canteiro();

            mockMvc.perform(estoque(aurora.getId()).header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(jsonPath("$.conteudo[*].descricao", contains("Serrote " + marca)))
                .andExpect(jsonPath("$.conteudo[0].saldo").value(4));
            mockMvc.perform(estoque(outro.getId()).header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(jsonPath("$.conteudo[0].saldo").value(0));
        }

        @Test
        @DisplayName("deve paginar sobre a lista já ordenada")
        void deve_paginar_quando_tamanhoInformado() throws Exception {
            for (int i = 1; i <= 5; i++) {
                item("Parafuso " + i, TipoItem.MATERIAL, "0", null);
            }

            mockMvc.perform(estoque(aurora.getId()).param("pagina", "2").param("tamanho", "2")
                    .header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.conteudo[*].descricao", contains("Parafuso 5 " + marca)));
        }

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE consultando outro canteiro e com 404 canteiro inexistente")
        void deve_recusar_quando_outroCanteiroOuInexistente() throws Exception {
            Canteiro outro = dados.canteiro();

            mockMvc.perform(estoque(outro.getId()).header("Authorization", tokens.bearer(almoxarife)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mockMvc.perform(estoque(UUID.randomUUID()).header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isNotFound());
        }
    }
}
