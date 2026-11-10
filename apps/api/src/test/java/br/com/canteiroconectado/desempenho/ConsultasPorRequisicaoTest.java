package br.com.canteiroconectado.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.colaborador.Colaborador;
import br.com.canteiroconectado.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.estoque.MovimentacaoService;
import br.com.canteiroconectado.estoque.NovaMovimentacao;
import br.com.canteiroconectado.estoque.TipoMovimentacao;
import br.com.canteiroconectado.item.Item;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.support.ContadorDeConsultas;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

import jakarta.persistence.EntityManager;

/**
 * Confere que as listas principais fazem o mesmo número de consultas com 1 e com 15 linhas na
 * página. Se alguma passar a buscar um dado por linha (N+1), o número cresce com a página e o
 * teste falha mostrando os SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({Tokens.class, Dados.class, Requisicoes.class, ContadorDeConsultas.class})
@DisplayName("Consultas por requisição (N+1)")
class ConsultasPorRequisicaoTest {

    private static final int MUITAS = 15;
    private static final int MAXIMO_POR_REQUISICAO = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private Dados dados;

    @Autowired
    private Requisicoes requisicoes;

    @Autowired
    private MovimentacaoService movimentacoes;

    @Autowired
    private AuditoriaService auditoria;

    @Autowired
    private ContadorDeConsultas contador;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private EntityManager entityManager;

    private Canteiro canteiro;
    private Usuario almoxarife;
    private Usuario gestor;
    private Usuario admin;
    private Colaborador colaborador;
    private String prefixo;

    @BeforeEach
    void preparar() {
        canteiro = dados.canteiro();
        almoxarife = tokens.almoxarife(canteiro.getId());
        gestor = tokens.gestor();
        admin = tokens.admin();
        colaborador = dados.colaborador(canteiro.getId());
        prefixo = Dados.unico("N1");
    }

    private List<String> consultasDe(Usuario usuario, MockHttpServletRequestBuilder pedido) throws Exception {
        entityManager.flush();
        entityManager.clear();
        contador.iniciar();
        mockMvc.perform(pedido.header("Authorization", tokens.bearer(usuario))).andExpect(status().isOk());
        return contador.parar();
    }

    private void conferirSemNMaisUm(Usuario usuario, MockHttpServletRequestBuilder pedido, Runnable acrescentar)
            throws Exception {
        acrescentar.run();
        List<String> comUma = consultasDe(usuario, pedido);
        for (int i = 1; i < MUITAS; i++) {
            acrescentar.run();
        }
        List<String> comMuitas = consultasDe(usuario, pedido);

        assertThat(comMuitas).as("as consultas não podem crescer com a página").hasSameSizeAs(comUma);
        assertThat(comUma.size()).as("nenhuma tela passa de %d consultas", MAXIMO_POR_REQUISICAO)
            .isLessThanOrEqualTo(MAXIMO_POR_REQUISICAO);
    }

    private UUID entrada(Item item, String quantidade) {
        return movimentacoes.aplicar(new NovaMovimentacao(UUID.randomUUID(), canteiro.getId(), item.getId(),
            TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, new BigDecimal(quantidade), null, null, null,
            null, almoxarife.getId())).getId();
    }

    private void saidaPara(Item item) {
        movimentacoes.aplicar(new NovaMovimentacao(UUID.randomUUID(), canteiro.getId(), item.getId(),
            TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, BigDecimal.ONE, colaborador.getId(), "Fundação",
            null, null, almoxarife.getId()));
    }

    @Nested
    @DisplayName("estoque e movimentações")
    class Estoque {

        @Test
        @DisplayName("estoque do canteiro (S06)")
        void estoque() throws Exception {
            conferirSemNMaisUm(almoxarife, get("/api/v1/canteiros/" + canteiro.getId() + "/estoque")
                .param("q", prefixo), () -> entrada(dados.item(prefixo + " " + UUID.randomUUID(), Unidade.UN,
                    TipoItem.MATERIAL, "5"), "2"));
        }

        @Test
        @DisplayName("detalhe do item com as últimas movimentações (S07)")
        void detalheDoItem() throws Exception {
            Item furadeira = dados.ferramenta();
            entrada(furadeira, "100");
            conferirSemNMaisUm(almoxarife, get("/api/v1/canteiros/" + canteiro.getId() + "/estoque/"
                + furadeira.getId()), () -> saidaPara(furadeira));
        }

        @Test
        @DisplayName("histórico de movimentações (S16)")
        void historico() throws Exception {
            Item furadeira = dados.ferramenta();
            entrada(furadeira, "100");
            conferirSemNMaisUm(gestor, get("/api/v1/movimentacoes").param("canteiroId", canteiro.getId().toString()),
                () -> saidaPara(furadeira));
        }

        @Test
        @DisplayName("resumo do Início (S04)")
        void resumo() throws Exception {
            conferirSemNMaisUm(gestor, get("/api/v1/canteiros/" + canteiro.getId() + "/estoque/resumo"),
                () -> dados.item(TipoItem.MATERIAL, "5"));
        }
    }

    @Nested
    @DisplayName("requisições")
    class RequisicoesDoCanteiro {

        @Test
        @DisplayName("lista de requisições com os itens de cada uma (S12)")
        void lista() throws Exception {
            conferirSemNMaisUm(gestor, get("/api/v1/requisicoes").param("canteiroId", canteiro.getId().toString()),
                () -> requisicoes.gravar(almoxarife, canteiro.getId(), "PENDENTE", Instant.now(),
                    dados.material().getId()));
        }
    }

    @Nested
    @DisplayName("cadastros e auditoria")
    class Cadastros {

        @Test
        @DisplayName("itens (S20)")
        void itens() throws Exception {
            conferirSemNMaisUm(almoxarife, get("/api/v1/itens").param("q", prefixo),
                () -> dados.item(prefixo + " " + UUID.randomUUID(), Unidade.UN,
                    TipoItem.MATERIAL, "0"));
        }

        @Test
        @DisplayName("colaboradores (S22)")
        void colaboradores() throws Exception {
            conferirSemNMaisUm(almoxarife, get("/api/v1/colaboradores").param("canteiroId", canteiro.getId().toString()),
                () -> dados.colaborador(canteiro.getId()));
        }

        @Test
        @DisplayName("funcionários (S24)")
        void funcionarios() throws Exception {
            conferirSemNMaisUm(admin, get("/api/v1/usuarios").param("q", prefixo),
                () -> usuarios.saveAndFlush(new Usuario(prefixo + " " + UUID.randomUUID(),
                    Dados.unico("n1") + "@canteiro.test", "x", Perfil.ALMOXARIFE, false, canteiro.getId())));
        }

        @Test
        @DisplayName("canteiros (S23)")
        void canteiros() throws Exception {
            conferirSemNMaisUm(admin, get("/api/v1/canteiros"), () -> dados.canteiro());
        }

        @Test
        @DisplayName("auditoria com o nome do autor e a descrição da entidade (S18)")
        void auditoria() throws Exception {
            conferirSemNMaisUm(gestor, get("/api/v1/auditoria").param("usuarioId", almoxarife.getId().toString()),
                () -> auditoria.registrar(almoxarife.getId(), AcaoAuditoria.ITEM_CRIADO, "item",
                    dados.material().getId(), Map.of()));
        }
    }
}
