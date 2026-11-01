package br.com.canteiroconectado.requisicao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class, Requisicoes.class})
@DisplayName("Regras do andamento da requisição")
class RegrasDoAndamentoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private Requisicoes requisicoes;

    private Canteiro aurora;
    private Usuario sebastiao;
    private Item cimento;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        sebastiao = tokens.almoxarife(aurora.getId());
        cimento = dados.material();
    }

    private ResultActions acao(Usuario usuario, UUID id, String acao) throws Exception {
        return mockMvc.perform(post("/api/v1/requisicoes/" + id + "/" + acao)
            .header("Authorization", tokens.bearer(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content(acao.equals("recusar") ? "{\"motivo\":\"Sem verba agora\"}" : "{}"));
    }

    @Nested
    @DisplayName("decisão")
    class Decisao {

        @Test
        @DisplayName("deve recusar com 403 o ALMOXARIFE decidindo a requisição de outro ALMOXARIFE do mesmo canteiro")
        void deve_recusar_quando_almoxarifeDecideDeAlmoxarife() throws Exception {
            UUID id = requisicoes.pendente(tokens.almoxarife(aurora.getId()), aurora.getId(), cimento.getId());

            acao(sebastiao, id, "aprovar")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.detail").value("As requisições do almoxarife são decididas pelo gestor."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"aprovar", "recusar", "atender", "cancelar"})
        @DisplayName("deve devolver 404 para requisição que não existe")
        void deve_devolver404_quando_requisicaoInexistente(String acao) throws Exception {
            acao(sebastiao, UUID.randomUUID(), acao)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
        }
    }

    @Nested
    @DisplayName("cancelamento")
    class Cancelamento {

        @ParameterizedTest
        @CsvSource({"RECUSADA, recusada", "ATENDIDA, atendida", "CANCELADA, cancelada"})
        @DisplayName("deve explicar o status quando o autor tenta cancelar a requisição já encerrada")
        void deve_explicarStatus_quando_requisicaoEncerrada(String status, String rotulo) throws Exception {
            UUID id = requisicoes.gravar(sebastiao, aurora.getId(), status, Instant.now(), cimento.getId());

            acao(sebastiao, id, "cancelar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_NAO_PENDENTE"))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value("Esta requisição já foi " + rotulo + " e não pode ser cancelada."));
        }
    }
}
