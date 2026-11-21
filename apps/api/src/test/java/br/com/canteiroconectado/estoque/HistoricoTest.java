package br.com.canteiroconectado.estoque;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
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
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("GET /api/v1/movimentacoes")
class HistoricoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private JdbcTemplate jdbc;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Usuario camila;
    private Item furadeira;
    private Item cimento;
    private Colaborador joao;
    private Colaborador maria;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        camila = tokens.gestor();
        furadeira = dados.ferramenta();
        cimento = dados.material();
        joao = dados.colaborador(aurora.getId());
        maria = dados.colaborador(aurora.getId());
    }

    private UUID gravar(Canteiro canteiro, Item item, String tipo, String motivo, Colaborador colaborador, String quando) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos,
                                              colaborador_id, registrado_por, criado_em)
            VALUES (?, ?, ?, ?, ?, 1, 1, ?, ?, ?)
            """, id, canteiro.getId(), item.getId(), tipo, motivo, colaborador == null ? null : colaborador.getId(),
            sebastiao.getId(), Timestamp.from(Instant.parse(quando)));
        return id;
    }

    private MockHttpServletRequestBuilder historico() {
        return get("/api/v1/movimentacoes").param("canteiroId", aurora.getId().toString())
            .header("Authorization", tokens.bearer(camila));
    }

    @Nested
    @DisplayName("filtros")
    class Filtros {

        @Test
        @DisplayName("deve listar da mais recente para a mais antiga com autor, colaborador, quantidade e horário")
        void deve_ordenarMaisRecentePrimeiro_quando_semFiltro() throws Exception {
            UUID antiga = gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-10-30T10:00:00Z");
            UUID recente = gravar(aurora, cimento, "ENTRADA", "RECEBIMENTO", null, "2026-11-01T09:00:00Z");
            gravar(dados.canteiro(), furadeira, "ENTRADA", "RECEBIMENTO", null, "2026-11-01T10:00:00Z");

            mockMvc.perform(historico())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[*].id", contains(recente.toString(), antiga.toString())))
                .andExpect(jsonPath("$.conteudo[1].registradoPorNome").value(sebastiao.getNome()))
                .andExpect(jsonPath("$.conteudo[1].colaboradorNome").value(joao.getNome()))
                .andExpect(jsonPath("$.conteudo[1].quantidade").value(1))
                .andExpect(jsonPath("$.conteudo[1].criadoEm").value("2026-10-30T10:00:00Z"));
        }

        @Test
        @DisplayName("deve combinar item e período de hoje, com o começo inclusive e o fim exclusive")
        void deve_combinarItemEPeriodo_quando_filtroDeHoje() throws Exception {
            UUID noComeco = gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-11-01T03:00:00Z");
            UUID durante = gravar(aurora, furadeira, "ENTRADA", "DEVOLUCAO", joao, "2026-11-01T18:00:00Z");
            gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-11-02T03:00:00Z");
            gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-10-31T23:00:00Z");
            gravar(aurora, cimento, "SAIDA", "RETIRADA", joao, "2026-11-01T12:00:00Z");

            mockMvc.perform(historico().param("itemId", furadeira.getId().toString())
                    .param("de", "2026-11-01T03:00:00Z").param("ate", "2026-11-02T03:00:00Z"))
                .andExpect(jsonPath("$.conteudo[*].id", contains(durante.toString(), noComeco.toString())));
        }

        @Test
        @DisplayName("deve combinar tipo e colaborador")
        void deve_combinarTipoEColaborador_quando_informados() throws Exception {
            UUID saidaDoJoao = gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-11-01T10:00:00Z");
            gravar(aurora, furadeira, "ENTRADA", "DEVOLUCAO", joao, "2026-11-01T11:00:00Z");
            gravar(aurora, furadeira, "SAIDA", "RETIRADA", maria, "2026-11-01T12:00:00Z");

            mockMvc.perform(historico().param("tipo", "SAIDA").param("colaboradorId", joao.getId().toString()))
                .andExpect(jsonPath("$.conteudo[*].id", contains(saidaDoJoao.toString())));
        }

        @Test
        @DisplayName("deve trazer ajustes e estornos com o motivo e o vínculo")
        void deve_trazerMotivoEVinculo_quando_ajusteEEstorno() throws Exception {
            UUID original = gravar(aurora, furadeira, "SAIDA", "RETIRADA", joao, "2026-11-01T10:00:00Z");
            jdbc.update("""
                INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos,
                                                  estorna_id, registrado_por, criado_em)
                VALUES (gen_random_uuid(), ?, ?, 'ENTRADA', 'ESTORNO', 1, 1, ?, ?, ?)
                """, aurora.getId(), furadeira.getId(), original, sebastiao.getId(),
                Timestamp.from(Instant.parse("2026-11-01T11:00:00Z")));
            gravar(aurora, cimento, "SAIDA", "AJUSTE", null, "2026-11-01T09:00:00Z");

            mockMvc.perform(historico())
                .andExpect(jsonPath("$.conteudo[*].motivo", contains("ESTORNO", "RETIRADA", "AJUSTE")))
                .andExpect(jsonPath("$.conteudo[0].estornaId").value(original.toString()))
                .andExpect(jsonPath("$.conteudo[1].estornadaPorId").isNotEmpty());
        }

        @Test
        @DisplayName("deve paginar e recusar período invertido")
        void deve_paginarERecusarPeriodoInvertido() throws Exception {
            for (int hora = 10; hora < 15; hora++) {
                gravar(aurora, cimento, "ENTRADA", "RECEBIMENTO", null, "2026-11-01T" + hora + ":00:00Z");
            }

            mockMvc.perform(historico().param("pagina", "1").param("tamanho", "2"))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.conteudo[0].criadoEm").value("2026-11-01T12:00:00Z"));
            mockMvc.perform(historico().param("de", "2026-11-02T00:00:00Z").param("ate", "2026-11-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("de"));
        }
    }

    @Nested
    @DisplayName("permissão")
    class Permissao {

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE, com 400 o GESTOR sem canteiro e com 401 sem token")
        void deve_recusar_quando_semPermissao() throws Exception {
            mockMvc.perform(get("/api/v1/movimentacoes").header("Authorization", tokens.bearer(sebastiao)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mockMvc.perform(get("/api/v1/movimentacoes").header("Authorization", tokens.bearer(camila)))
                .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/v1/movimentacoes")).andExpect(status().isUnauthorized());
        }
    }
}
