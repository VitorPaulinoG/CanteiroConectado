package br.com.canteiroconectado.estoque;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("Regras da entrada")
class EntradaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Colaborador joao;
    private Item cimento;
    private Item furadeira;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        joao = dados.colaborador(aurora.getId());
        cimento = dados.item("Cimento CP-II 50kg", Unidade.SACO, TipoItem.MATERIAL, "50");
        furadeira = dados.ferramenta();
    }

    private ResultActions entrada(Item item, String motivo, String quantidade, UUID colaboradorId, String observacao)
            throws Exception {
        String corpo = "{\"id\":\"" + UUID.randomUUID() + "\",\"tipo\":\"ENTRADA\",\"motivo\":\"" + motivo + "\","
            + "\"itemId\":\"" + item.getId() + "\",\"quantidade\":" + quantidade
            + (colaboradorId == null ? "" : ",\"colaboradorId\":\"" + colaboradorId + "\"")
            + (observacao == null ? "" : ",\"observacao\":\"" + observacao + "\"") + "}";
        return mockMvc.perform(post("/api/v1/canteiros/" + aurora.getId() + "/movimentacoes")
            .header("Authorization", tokens.bearer(sebastiao))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("recebimento")
    class Recebimento {

        @Test
        @DisplayName("deve somar 50 sacos de cimento e mostrar quem registrou, com observação opcional")
        void deve_somar_quando_recebimento() throws Exception {
            entrada(cimento, "RECEBIMENTO", "50", null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(50))
                .andExpect(jsonPath("$.unidadeSimbolo").value("saco"))
                .andExpect(jsonPath("$.registradoPorNome").value(sebastiao.getNome()));
            entrada(cimento, "RECEBIMENTO", "10", null, "NF 778 - Depósito Central")
                .andExpect(jsonPath("$.saldoApos").value(60))
                .andExpect(jsonPath("$.observacao").value("NF 778 - Depósito Central"));
        }

        @Test
        @DisplayName("deve recusar quantidade zero, negativa e observação acima de 200 caracteres")
        void deve_recusar_quando_quantidadeOuObservacaoInvalidas() throws Exception {
            entrada(cimento, "RECEBIMENTO", "0", null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("QUANTIDADE_INVALIDA"));
            entrada(cimento, "RECEBIMENTO", "-5", null, null)
                .andExpect(jsonPath("$.codigo").value("QUANTIDADE_INVALIDA"));
            entrada(cimento, "RECEBIMENTO", "1", null, "x".repeat(201))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("devolução")
    class Devolucao {

        @Test
        @DisplayName("deve somar 1 furadeira e registrar o colaborador que devolveu")
        void deve_somarERegistrarColaborador_quando_devolucaoDeFerramenta() throws Exception {
            entrada(furadeira, "DEVOLUCAO", "1", joao.getId(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(1))
                .andExpect(jsonPath("$.motivo").value("DEVOLUCAO"))
                .andExpect(jsonPath("$.colaboradorNome").value(joao.getNome()));
        }

        @Test
        @DisplayName("deve exigir o colaborador")
        void deve_exigirColaborador_quando_devolucao() throws Exception {
            entrada(furadeira, "DEVOLUCAO", "1", null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("COLABORADOR_OBRIGATORIO"));
        }

        @Test
        @DisplayName("deve recusar com DEVOLUCAO_SO_FERRAMENTA a devolução de material")
        void deve_recusar_quando_devolucaoDeMaterial() throws Exception {
            entrada(cimento, "DEVOLUCAO", "1", joao.getId(), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DEVOLUCAO_SO_FERRAMENTA"))
                .andExpect(jsonPath("$.campo").value("itemId"));
        }
    }

    @Nested
    @DisplayName("ajuste")
    class Ajuste {

        @Test
        @DisplayName("deve exigir observação e aceitar quando ela vem")
        void deve_exigirObservacao_quando_ajuste() throws Exception {
            entrada(cimento, "AJUSTE", "2", null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("OBSERVACAO_OBRIGATORIA"));
            entrada(cimento, "AJUSTE", "2", null, "Sobra na contagem de sexta")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(2));
        }
    }
}
