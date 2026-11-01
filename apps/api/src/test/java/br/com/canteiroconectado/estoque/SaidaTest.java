package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Regras da saída")
class SaidaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private MovimentacaoService movimentacoes;

    @Autowired
    private EntityManager entityManager;

    private Canteiro aurora;
    private Item furadeira;
    private Usuario sebastiao;
    private Colaborador joao;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        furadeira = dados.ferramenta();
        sebastiao = tokens.almoxarife(aurora.getId());
        joao = dados.colaborador(aurora.getId());
        movimentacoes.aplicar(new NovaMovimentacao(UUID.randomUUID(), aurora.getId(), furadeira.getId(),
            TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, new BigDecimal("2"), null, null, null, null,
            sebastiao.getId()));
    }

    private ResultActions saida(String motivo, String quantidade, UUID colaboradorId, String frente, String observacao)
            throws Exception {
        String corpo = "{\"id\":\"" + UUID.randomUUID() + "\",\"tipo\":\"SAIDA\",\"motivo\":\"" + motivo + "\","
            + "\"itemId\":\"" + furadeira.getId() + "\",\"quantidade\":" + quantidade
            + (colaboradorId == null ? "" : ",\"colaboradorId\":\"" + colaboradorId + "\"")
            + (frente == null ? "" : ",\"frenteObra\":\"" + frente + "\"")
            + (observacao == null ? "" : ",\"observacao\":\"" + observacao + "\"") + "}";
        return mockMvc.perform(post("/api/v1/canteiros/" + aurora.getId() + "/movimentacoes")
            .header("Authorization", tokens.bearer(sebastiao))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("retirada")
    class Retirada {

        @Test
        @DisplayName("deve baixar o saldo de 2 para 1 com colaborador e frente, como no cenário do João na Fundação")
        void deve_baixarSaldo_quando_retiradaCompleta() throws Exception {
            saida("RETIRADA", "1", joao.getId(), "Fundação", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(1))
                .andExpect(jsonPath("$.colaboradorNome").value(joao.getNome()))
                .andExpect(jsonPath("$.frenteObra").value("Fundação"))
                .andExpect(jsonPath("$.registradoPorNome").value(sebastiao.getNome()));
            entityManager.flush();
            assertThat(dados.saldo(aurora.getId(), furadeira.getId())).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("deve recusar com SALDO_INSUFICIENTE a quantidade acima do saldo")
        void deve_recusar_quando_acimaDoSaldo() throws Exception {
            saida("RETIRADA", "3", joao.getId(), "Fundação", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.saldoAtual").value(2));
        }

        @Test
        @DisplayName("deve exigir colaborador e frente de obra")
        void deve_exigirColaboradorEFrente_quando_retirada() throws Exception {
            saida("RETIRADA", "1", null, "Fundação", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("COLABORADOR_OBRIGATORIO"))
                .andExpect(jsonPath("$.campo").value("colaboradorId"));
            saida("RETIRADA", "1", joao.getId(), "  ", null)
                .andExpect(jsonPath("$.codigo").value("FRENTE_OBRA_OBRIGATORIA"))
                .andExpect(jsonPath("$.campo").value("frenteObra"));
        }

        @Test
        @DisplayName("deve recusar com COLABORADOR_INVALIDO o colaborador de outro canteiro ou inativo")
        void deve_recusar_quando_colaboradorNaoServe() throws Exception {
            Colaborador deOutro = dados.colaborador(dados.canteiro().getId());
            Colaborador inativo = dados.colaborador(aurora.getId());
            inativo.setAtivo(false);
            entityManager.flush();

            saida("RETIRADA", "1", deOutro.getId(), "Fundação", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("COLABORADOR_INVALIDO"));
            saida("RETIRADA", "1", inativo.getId(), "Fundação", null)
                .andExpect(jsonPath("$.codigo").value("COLABORADOR_INVALIDO"));
        }

        @Test
        @DisplayName("deve recusar com ITEM_INATIVO o item desativado")
        void deve_recusar_quando_itemInativo() throws Exception {
            furadeira.setAtivo(false);
            entityManager.flush();

            saida("RETIRADA", "1", joao.getId(), "Fundação", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ITEM_INATIVO"));
        }
    }

    @Nested
    @DisplayName("ajuste")
    class Ajuste {

        @Test
        @DisplayName("deve aceitar sem colaborador e exigir a observação")
        void deve_exigirObservacao_quando_ajuste() throws Exception {
            saida("AJUSTE", "1", null, null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("OBSERVACAO_OBRIGATORIA"));
            saida("AJUSTE", "1", null, null, "Furto no fim de semana")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo").value("AJUSTE"))
                .andExpect(jsonPath("$.colaboradorId").doesNotExist())
                .andExpect(jsonPath("$.saldoApos").value(1));
        }
    }
}
