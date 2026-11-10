package br.com.canteiroconectado.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, ItemControllerTest.UsoFalso.class})
@DisplayName("ItemController")
class ItemControllerTest {

    static final Set<UUID> MOVIMENTADOS = new HashSet<>();

    @TestConfiguration
    static class UsoFalso {

        @Bean
        UsoDoItem usoFalso() {
            return MOVIMENTADOS::contains;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private ItemRepository itens;

    @Autowired
    private LogAuditoriaRepository logs;

    @AfterEach
    void limpar() {
        MOVIMENTADOS.clear();
    }

    private static final String FURADEIRA = """
        {"codigo":" FER-0012 ","descricao":"Furadeira de impacto 700W","unidade":"UN","tipo":"FERRAMENTA","quantidadeMinima":1}
        """;

    private ResultActions criar(Usuario autor, String corpo) throws Exception {
        return mockMvc.perform(post("/api/v1/itens")
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    private ResultActions alterar(Usuario autor, UUID id, String corpo) throws Exception {
        return mockMvc.perform(patch("/api/v1/itens/" + id)
            .header("Authorization", tokens.bearer(autor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    private Item cimento() {
        return itens.saveAndFlush(new Item("MAT-0001", "Cimento CP-II 50kg", Unidade.SACO, TipoItem.MATERIAL,
            new BigDecimal("50")));
    }

    @Nested
    @DisplayName("POST /api/v1/itens")
    class Criar {

        @Test
        @DisplayName("deve cadastrar a furadeira do cenário pelo ALMOXARIFE e registrar ITEM_CRIADO")
        void deve_cadastrar_quando_almoxarife() throws Exception {
            criar(tokens.almoxarife(), FURADEIRA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("FER-0012"))
                .andExpect(jsonPath("$.unidade").value("UN"))
                .andExpect(jsonPath("$.unidadeSimbolo").value("un"))
                .andExpect(jsonPath("$.tipo").value("FERRAMENTA"))
                .andExpect(jsonPath("$.quantidadeMinima").value(1))
                .andExpect(jsonPath("$.ativo").value(true));

            Item criado = itens.findAll().stream().filter(i -> i.getCodigo().equals("FER-0012")).findFirst()
                .orElseThrow();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(criado.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.ITEM_CRIADO);
        }

        @Test
        @DisplayName("deve aceitar o GESTOR e usar mínimo zero quando não informado")
        void deve_usarMinimoZero_quando_naoInformado() throws Exception {
            criar(tokens.gestor(), """
                {"codigo":"MAT-9","descricao":"Areia média","unidade":"M3","tipo":"MATERIAL"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeMinima").value(0))
                .andExpect(jsonPath("$.unidadeSimbolo").value("m³"));
        }

        @Test
        @DisplayName("deve recusar com 409 CODIGO_DUPLICADO o código com maiúsculas e espaços diferentes")
        void deve_recusar_quando_codigoDuplicado() throws Exception {
            criar(tokens.almoxarife(), FURADEIRA).andExpect(status().isCreated());

            criar(tokens.gestor(), FURADEIRA.replace(" FER-0012 ", "fer-0012  "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"))
                .andExpect(jsonPath("$.detail").value("Código já cadastrado."))
                .andExpect(jsonPath("$.campo").value("codigo"));
        }

        @Test
        @DisplayName("deve recusar unidade fora da lista e mínimo negativo")
        void deve_recusar_quando_unidadeOuMinimoInvalidos() throws Exception {
            criar(tokens.almoxarife(), FURADEIRA.replace("\"UN\"", "\"TONELADA\""))
                .andExpect(status().isBadRequest());
            criar(tokens.almoxarife(), FURADEIRA.replace("\"quantidadeMinima\":1", "\"quantidadeMinima\":-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }

        @Test
        @DisplayName("deve exigir token")
        void deve_exigirToken_quando_semToken() throws Exception {
            mockMvc.perform(post("/api/v1/itens").contentType(MediaType.APPLICATION_JSON).content(FURADEIRA))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/itens/{id}")
    class Atualizar {

        @Test
        @DisplayName("deve desativar pelo campo ativo e registrar ITEM_EDITADO")
        void deve_desativar_quando_ativoFalso() throws Exception {
            Item item = cimento();

            alterar(tokens.gestor(), item.getId(), "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(item.getId())).extracting(LogAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.ITEM_EDITADO);
        }

        @Test
        @DisplayName("deve permitir mudar código e unidade antes da primeira movimentação")
        void deve_permitirCodigoEUnidade_quando_semMovimentacao() throws Exception {
            Item item = cimento();

            alterar(tokens.almoxarife(), item.getId(), "{\"codigo\":\"MAT-0002\",\"unidade\":\"KG\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("MAT-0002"))
                .andExpect(jsonPath("$.unidade").value("KG"));
        }

        @Test
        @DisplayName("deve recusar com 422 ITEM_JA_MOVIMENTADO mudar código ou unidade depois da movimentação")
        void deve_recusar_quando_itemJaMovimentado() throws Exception {
            Item item = cimento();
            MOVIMENTADOS.add(item.getId());

            alterar(tokens.almoxarife(), item.getId(), "{\"codigo\":\"MAT-0002\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ITEM_JA_MOVIMENTADO"))
                .andExpect(jsonPath("$.campo").value("codigo"));
            alterar(tokens.almoxarife(), item.getId(), "{\"unidade\":\"KG\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.campo").value("unidade"));
            alterar(tokens.almoxarife(), item.getId(), "{\"descricao\":\"Cimento CP-II 50 kg\",\"quantidadeMinima\":40}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeMinima").value(40));
        }

        @Test
        @DisplayName("deve recusar renomear o código para um já existente")
        void deve_recusar_quando_codigoDeOutroItem() throws Exception {
            cimento();
            Item outro = itens.saveAndFlush(new Item("MAT-0003", "Brita", Unidade.M3, TipoItem.MATERIAL, BigDecimal.ZERO));

            alterar(tokens.almoxarife(), outro.getId(), "{\"codigo\":\"mat-0001\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/itens")
    class Listar {

        @Test
        @DisplayName("deve listar para qualquer perfil filtrando tipo e situação")
        void deve_filtrar_quando_tipoEAtivo() throws Exception {
            cimento();
            Item furadeira = itens.saveAndFlush(new Item("FER-0012", "Furadeira de impacto 700W", Unidade.UN,
                TipoItem.FERRAMENTA, BigDecimal.ONE));
            Item inativa = itens.saveAndFlush(new Item("FER-0099", "Furadeira antiga", Unidade.UN,
                TipoItem.FERRAMENTA, BigDecimal.ZERO));
            inativa.setAtivo(false);

            mockMvc.perform(get("/api/v1/itens").param("tipo", "FERRAMENTA").param("ativo", "true")
                    .header("Authorization", tokens.bearer(tokens.almoxarife())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].codigo", contains("FER-0012")));
            mockMvc.perform(get("/api/v1/itens/" + furadeira.getId())
                    .header("Authorization", tokens.bearer(tokens.gestor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Furadeira de impacto 700W"));
        }
    }
}
