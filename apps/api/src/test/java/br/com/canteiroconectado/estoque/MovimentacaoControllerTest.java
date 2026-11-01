package br.com.canteiroconectado.estoque;

import static org.assertj.core.api.Assertions.assertThat;
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
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("MovimentacaoController")
class MovimentacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private EntityManager entityManager;

    private Canteiro aurora;
    private Item cimento;
    private Usuario sebastiao;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        cimento = dados.material();
        sebastiao = tokens.almoxarife(aurora.getId());
    }

    private String corpo(String id, String tipo, String motivo, String quantidade, String extra) {
        return "{\"id\":" + (id == null ? "null" : "\"" + id + "\"") + ",\"tipo\":\"" + tipo + "\",\"motivo\":\""
            + motivo + "\",\"itemId\":\"" + cimento.getId() + "\",\"quantidade\":" + quantidade + extra + "}";
    }

    private ResultActions registrar(Usuario autor, UUID canteiroId, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/canteiros/" + canteiroId + "/movimentacoes")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("POST /api/v1/canteiros/{id}/movimentacoes")
    class Registrar {

        @Test
        @DisplayName("deve gravar a entrada com 201, devolver nomes e saldo_apos e atualizar o saldo")
        void deve_gravarEntrada_quando_almoxarifeDoCanteiro() throws Exception {
            String id = UUID.randomUUID().toString();

            registrar(sebastiao, aurora.getId(), corpo(id, "ENTRADA", "RECEBIMENTO", "50",
                    ",\"observacao\":\" NF 4512 \""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.quantidade").value(50))
                .andExpect(jsonPath("$.saldoApos").value(50))
                .andExpect(jsonPath("$.itemCodigo").value(cimento.getCodigo()))
                .andExpect(jsonPath("$.canteiroNome").value(aurora.getNome()))
                .andExpect(jsonPath("$.registradoPorNome").value(sebastiao.getNome()))
                .andExpect(jsonPath("$.observacao").value("NF 4512"))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty());
            entityManager.flush();
            assertThat(dados.saldo(aurora.getId(), cimento.getId())).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("deve devolver 200 com o mesmo registro quando o pedido se repete, sem mudar o saldo")
        void deve_devolverRegistro_quando_pedidoRepetido() throws Exception {
            String pedido = corpo(UUID.randomUUID().toString(), "ENTRADA", "RECEBIMENTO", "10", "");
            registrar(sebastiao, aurora.getId(), pedido).andExpect(status().isCreated());

            registrar(sebastiao, aurora.getId(), pedido)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoApos").value(10));
            entityManager.flush();
            assertThat(dados.saldo(aurora.getId(), cimento.getId())).isEqualByComparingTo("10");
            assertThat(dados.contarMovimentacoes(aurora.getId(), cimento.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("deve recusar com 409 ID_JA_UTILIZADO o mesmo id com outro conteúdo")
        void deve_recusar_quando_idComOutroConteudo() throws Exception {
            String id = UUID.randomUUID().toString();
            registrar(sebastiao, aurora.getId(), corpo(id, "ENTRADA", "RECEBIMENTO", "10", ""))
                .andExpect(status().isCreated());

            registrar(sebastiao, aurora.getId(), corpo(id, "ENTRADA", "RECEBIMENTO", "11", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ID_JA_UTILIZADO"))
                .andExpect(jsonPath("$.campo").value("id"));
        }

        @Test
        @DisplayName("deve recusar com 400 ID_INVALIDO o id ausente ou fora do formato")
        void deve_recusar_quando_idInvalido() throws Exception {
            registrar(sebastiao, aurora.getId(), corpo(null, "ENTRADA", "RECEBIMENTO", "1", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ID_INVALIDO"));
            registrar(sebastiao, aurora.getId(), corpo("abc", "ENTRADA", "RECEBIMENTO", "1", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ID_INVALIDO"));
        }

        @Test
        @DisplayName("deve recusar com 422 SALDO_INSUFICIENTE e o saldo atual")
        void deve_recusar_quando_saldoInsuficiente() throws Exception {
            registrar(sebastiao, aurora.getId(), corpo(UUID.randomUUID().toString(), "SAIDA", "AJUSTE", "1",
                    ",\"observacao\":\"Perda\""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.saldoAtual").value(0));
        }

        @Test
        @DisplayName("deve recusar com 422 MOTIVO_INVALIDO o motivo de outro tipo e o estorno direto")
        void deve_recusar_quando_motivoInvalido() throws Exception {
            registrar(sebastiao, aurora.getId(), corpo(UUID.randomUUID().toString(), "ENTRADA", "RETIRADA", "1", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_INVALIDO"));
            registrar(sebastiao, aurora.getId(), corpo(UUID.randomUUID().toString(), "ENTRADA", "ESTORNO", "1", ""))
                .andExpect(jsonPath("$.codigo").value("MOTIVO_INVALIDO"));
        }

        @Test
        @DisplayName("deve recusar com 400 frente de obra acima de 60 caracteres")
        void deve_recusar_quando_frenteLonga() throws Exception {
            registrar(sebastiao, aurora.getId(), corpo(UUID.randomUUID().toString(), "ENTRADA", "RECEBIMENTO", "1",
                    ",\"frenteObra\":\"" + "x".repeat(61) + "\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }

        @Test
        @DisplayName("deve recusar com 403 o GESTOR e o ALMOXARIFE de outro canteiro, e com 401 sem token")
        void deve_recusar_quando_semPermissao() throws Exception {
            String pedido = corpo(UUID.randomUUID().toString(), "ENTRADA", "RECEBIMENTO", "1", "");

            registrar(tokens.gestor(), aurora.getId(), pedido)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            registrar(tokens.almoxarife(), aurora.getId(), pedido).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/canteiros/" + aurora.getId() + "/movimentacoes")
                    .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve recusar com 422 CANTEIRO_INATIVO")
        void deve_recusar_quando_canteiroInativo() throws Exception {
            aurora.setAtivo(false);

            registrar(sebastiao, aurora.getId(), corpo(UUID.randomUUID().toString(), "ENTRADA", "RECEBIMENTO", "1", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_INATIVO"));
        }
    }
}
