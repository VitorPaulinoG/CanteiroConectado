package br.com.canteiroconectado.requisicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class})
@DisplayName("POST /api/v1/requisicoes")
class CriarRequisicaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private LogAuditoriaRepository logs;

    @Autowired
    private EntityManager entityManager;

    private Canteiro aurora;
    private Usuario camila;
    private Item cimento;
    private LocalDate amanha;

    @BeforeEach
    void preparar() {
        aurora = dados.canteiro();
        camila = tokens.gestor();
        cimento = dados.item("Cimento CP-II 50kg", Unidade.SACO, TipoItem.MATERIAL, "50");
        amanha = LocalDate.now(Fuso.DAS_OBRAS).plusDays(1);
    }

    private String corpo(String id, UUID canteiroId, LocalDate necessarioAte, String itens, String extra) {
        return "{\"id\":" + (id == null ? "null" : "\"" + id + "\"")
            + (canteiroId == null ? "" : ",\"canteiroId\":\"" + canteiroId + "\"")
            + ",\"necessarioAte\":\"" + necessarioAte + "\",\"itens\":[" + itens + "]" + extra + "}";
    }

    private String item(Item item, String quantidade) {
        return "{\"itemId\":\"" + item.getId() + "\",\"quantidade\":" + quantidade + "}";
    }

    private ResultActions criar(Usuario autor, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/requisicoes")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Nested
    @DisplayName("criação")
    class Criacao {

        @Test
        @DisplayName("deve criar como PENDENTE com 40 sacos de cimento, como no cenário da Camila, e registrar REQUISICAO_CRIADA")
        void deve_criarPendente_quando_gestorPedeCimento() throws Exception {
            String id = UUID.randomUUID().toString();

            criar(camila, corpo(id, aurora.getId(), amanha, item(cimento, "40"),
                    ",\"frenteObra\":\" Laje do 3º andar \",\"observacao\":\"Urgente\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.numero").value("REQ-0001"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.solicitanteNome").value(camila.getNome()))
                .andExpect(jsonPath("$.solicitantePerfil").value("GESTOR"))
                .andExpect(jsonPath("$.necessarioAte").value(amanha.toString()))
                .andExpect(jsonPath("$.frenteObra").value("Laje do 3º andar"))
                .andExpect(jsonPath("$.itens[0].descricao").value("Cimento CP-II 50kg"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(40))
                .andExpect(jsonPath("$.itens[0].unidadeSimbolo").value("saco"))
                .andExpect(jsonPath("$.historico[*].status", contains("PENDENTE")))
                .andExpect(jsonPath("$.acoes.cancelar").value(true))
                .andExpect(jsonPath("$.acoes.decidir").value(false));

            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(UUID.fromString(id))).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.REQUISICAO_CRIADA);
        }

        @Test
        @DisplayName("deve numerar em sequência por canteiro")
        void deve_numerarPorCanteiro_quando_variasRequisicoes() throws Exception {
            Canteiro outro = dados.canteiro();
            Usuario almoxarife = tokens.almoxarife(aurora.getId());

            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, item(cimento, "1"), ""))
                .andExpect(jsonPath("$.numero").value("REQ-0001"));
            criar(almoxarife, corpo(UUID.randomUUID().toString(), null, amanha, item(cimento, "1"), ""))
                .andExpect(jsonPath("$.numero").value("REQ-0002"))
                .andExpect(jsonPath("$.canteiroId").value(aurora.getId().toString()));
            criar(camila, corpo(UUID.randomUUID().toString(), outro.getId(), amanha, item(cimento, "1"), ""))
                .andExpect(jsonPath("$.numero").value("REQ-0001"));
        }

        @Test
        @DisplayName("deve aceitar a data de hoje e vários itens na ordem informada")
        void deve_aceitarHoje_quando_variosItens() throws Exception {
            Item areia = dados.item("Areia média", Unidade.M3, TipoItem.MATERIAL, "0");

            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), LocalDate.now(Fuso.DAS_OBRAS),
                    item(cimento, "40") + "," + item(areia, "2.5"), ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[1].quantidade").value(2.5));
        }
    }

    @Nested
    @DisplayName("validações")
    class Validacoes {

        @Test
        @DisplayName("deve recusar a data de necessidade anterior a hoje")
        void deve_recusar_quando_dataNoPassado() throws Exception {
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), LocalDate.now(Fuso.DAS_OBRAS).minusDays(1),
                    item(cimento, "40"), ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DATA_NO_PASSADO"))
                .andExpect(jsonPath("$.campo").value("necessarioAte"));
        }

        @Test
        @DisplayName("deve exigir de 1 a 20 itens")
        void deve_exigirDeUmAVinteItens_quando_listaForaDoLimite() throws Exception {
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, "", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_SEM_ITENS"));

            List<String> vinteEUm = new ArrayList<>();
            for (int i = 0; i < 21; i++) {
                vinteEUm.add(item(dados.material(), "1"));
            }
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha,
                    vinteEUm.stream().collect(Collectors.joining(",")), ""))
                .andExpect(jsonPath("$.codigo").value("LIMITE_DE_ITENS"));
        }

        @Test
        @DisplayName("deve recusar item repetido, quantidade zero e item inativo apontando a linha")
        void deve_recusar_quando_itemInvalido() throws Exception {
            Item inativo = dados.material();
            inativo.setAtivo(false);
            entityManager.flush();

            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha,
                    item(cimento, "1") + "," + item(cimento, "2"), ""))
                .andExpect(jsonPath("$.codigo").value("ITEM_REPETIDO"))
                .andExpect(jsonPath("$.campo").value("itens[1].itemId"));
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, item(cimento, "0"), ""))
                .andExpect(jsonPath("$.codigo").value("QUANTIDADE_INVALIDA"))
                .andExpect(jsonPath("$.campo").value("itens[0].quantidade"));
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha,
                    item(cimento, "1") + "," + item(inativo, "1"), ""))
                .andExpect(jsonPath("$.codigo").value("ITEM_INATIVO"))
                .andExpect(jsonPath("$.campo").value("itens[1].itemId"));
        }
    }

    @Nested
    @DisplayName("id do cliente")
    class IdDoCliente {

        @Test
        @DisplayName("deve devolver a requisição já criada com 200 quando o mesmo pedido se repete")
        void deve_devolverCriada_quando_pedidoRepetido() throws Exception {
            String pedido = corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, item(cimento, "40"), "");
            criar(camila, pedido).andExpect(status().isCreated());

            criar(camila, pedido)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("REQ-0001"));
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, item(cimento, "1"), ""))
                .andExpect(jsonPath("$.numero").value("REQ-0002"));
        }

        @Test
        @DisplayName("deve recusar com 409 ID_JA_UTILIZADO o mesmo id com outro conteúdo, e com 400 o id inválido")
        void deve_recusar_quando_idDivergenteOuInvalido() throws Exception {
            String id = UUID.randomUUID().toString();
            criar(camila, corpo(id, aurora.getId(), amanha, item(cimento, "40"), "")).andExpect(status().isCreated());

            criar(camila, corpo(id, aurora.getId(), amanha, item(cimento, "41"), ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ID_JA_UTILIZADO"));
            criar(camila, corpo("nao-e-uuid", aurora.getId(), amanha, item(cimento, "1"), ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ID_INVALIDO"));
        }
    }

    @Nested
    @DisplayName("permissão")
    class Permissao {

        @Test
        @DisplayName("deve recusar o ALMOXARIFE em outro canteiro, o GESTOR sem canteiro e o canteiro inativo")
        void deve_recusar_quando_canteiroNaoPermitido() throws Exception {
            Canteiro outro = dados.canteiro();

            criar(tokens.almoxarife(aurora.getId()), corpo(UUID.randomUUID().toString(), outro.getId(), amanha,
                    item(cimento, "1"), ""))
                .andExpect(status().isForbidden());
            criar(camila, corpo(UUID.randomUUID().toString(), null, amanha, item(cimento, "1"), ""))
                .andExpect(status().isBadRequest());
            aurora.setAtivo(false);
            entityManager.flush();
            criar(camila, corpo(UUID.randomUUID().toString(), aurora.getId(), amanha, item(cimento, "1"), ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CANTEIRO_INATIVO"));
        }
    }
}
