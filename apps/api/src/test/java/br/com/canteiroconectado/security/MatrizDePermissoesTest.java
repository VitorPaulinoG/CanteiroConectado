package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.estoque.MovimentacaoService;
import br.com.canteiroconectado.estoque.NovaMovimentacao;
import br.com.canteiroconectado.estoque.TipoMovimentacao;
import br.com.canteiroconectado.support.Dados;
import br.com.canteiroconectado.support.Requisicoes;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

/**
 * Matriz de permissões da história E05-H3: cada endpoint da API chamado por cada perfil, conferindo
 * o resultado esperado.
 *
 * <ul>
 *   <li>{@code P} (permitido): a resposta não é 401 nem 403 e não é erro do servidor. Pode ser 200,
 *   201, 409 ou 422, conforme o estado dos dados; o que a matriz testa é o acesso.</li>
 *   <li>{@code N} (negado): 403, com {@code ACESSO_NEGADO} ou {@code AUTOAPROVACAO_PROIBIDA}.</li>
 *   <li>Sem token, todo endpoint não público responde 401 {@code NAO_AUTENTICADO}.</li>
 * </ul>
 *
 * Os perfis são o ALMOXARIFE do canteiro A, o ALMOXARIFE do canteiro B (testa o acesso entre
 * canteiros), o GESTOR e o GESTOR administrador. Cada chamada roda numa transação desfeita no fim,
 * então ações como desativar ou aprovar não afetam as linhas seguintes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({Tokens.class, Dados.class, Requisicoes.class})
@DisplayName("Matriz de permissões")
class MatrizDePermissoesTest {

    enum Perfil {
        ALMOXARIFE_DO_CANTEIRO(0),
        ALMOXARIFE_DE_OUTRO_CANTEIRO(1),
        GESTOR(2),
        ADMINISTRADOR(3);

        final int coluna;

        Perfil(int coluna) {
            this.coluna = coluna;
        }
    }

    record Cenario(UUID canteiroA, Usuario almoxarifeA, Usuario almoxarifeB, Usuario gestor, Usuario admin,
            Usuario alvo, UUID item, UUID colaboradorA, UUID movimentacaoA, UUID requisicaoDoGestor,
            UUID requisicaoDoAlmoxarife, UUID requisicaoAprovada) {

        Usuario usuario(Perfil perfil) {
            return switch (perfil) {
                case ALMOXARIFE_DO_CANTEIRO -> almoxarifeA;
                case ALMOXARIFE_DE_OUTRO_CANTEIRO -> almoxarifeB;
                case GESTOR -> gestor;
                case ADMINISTRADOR -> admin;
            };
        }
    }

    record Linha(String endpoint, String esperado, Function<Cenario, MockHttpServletRequestBuilder> pedido) {

        @Override
        public String toString() {
            return endpoint;
        }
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String corpo) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static String amanha() {
        return LocalDate.now(Fuso.DAS_OBRAS).plusDays(1).toString();
    }

    static final List<Linha> MATRIZ = List.of(
        new Linha("GET /autenticacao/eu", "PPPP", c -> get("/api/v1/autenticacao/eu")),
        new Linha("POST /autenticacao/trocar-senha", "PPPP", c -> json(post("/api/v1/autenticacao/trocar-senha"),
            "{\"senhaAtual\":\"nao-e-esta-1\",\"novaSenha\":\"outra-senha-9\"}")),

        new Linha("GET /canteiros", "PPPP", c -> get("/api/v1/canteiros")),
        new Linha("GET /canteiros/{id}", "PNPP", c -> get("/api/v1/canteiros/" + c.canteiroA())),
        new Linha("POST /canteiros", "NNNP", c -> json(post("/api/v1/canteiros"),
            "{\"nome\":\"Matriz " + UUID.randomUUID() + "\"}")),
        new Linha("PATCH /canteiros/{id}", "NNNP", c -> json(patch("/api/v1/canteiros/" + c.canteiroA()),
            "{\"endereco\":\"Rua da Matriz\"}")),

        new Linha("GET /usuarios", "NNNP", c -> get("/api/v1/usuarios")),
        new Linha("GET /usuarios/{id}", "NNNP", c -> get("/api/v1/usuarios/" + c.alvo().getId())),
        new Linha("POST /usuarios", "NNNP", c -> json(post("/api/v1/usuarios"), "{\"nome\":\"Novo\",\"email\":\"novo."
            + UUID.randomUUID().toString().substring(0, 8) + "@obra.com\",\"perfil\":\"GESTOR\"}")),
        new Linha("PATCH /usuarios/{id}", "NNNP", c -> json(patch("/api/v1/usuarios/" + c.alvo().getId()),
            "{\"nome\":\"Nome novo\"}")),
        new Linha("POST /usuarios/{id}/redefinir-senha", "NNNP",
            c -> post("/api/v1/usuarios/" + c.alvo().getId() + "/redefinir-senha")),
        new Linha("GET /usuarios/{id}/credencial-inicial", "NNNP",
            c -> get("/api/v1/usuarios/" + c.alvo().getId() + "/credencial-inicial")),
        new Linha("POST /usuarios/{id}/desativar", "NNNP",
            c -> post("/api/v1/usuarios/" + c.alvo().getId() + "/desativar")),
        new Linha("POST /usuarios/{id}/ativar", "NNNP", c -> post("/api/v1/usuarios/" + c.alvo().getId() + "/ativar")),

        new Linha("GET /colaboradores", "PNPP",
            c -> get("/api/v1/colaboradores").param("canteiroId", c.canteiroA().toString())),
        new Linha("POST /colaboradores", "PNPP", c -> json(post("/api/v1/colaboradores"),
            "{\"nome\":\"Colaborador " + UUID.randomUUID() + "\",\"canteiroId\":\"" + c.canteiroA() + "\"}")),
        new Linha("PATCH /colaboradores/{id}", "PNPP", c -> json(patch("/api/v1/colaboradores/" + c.colaboradorA()),
            "{\"nome\":\"Colaborador " + UUID.randomUUID() + "\"}")),

        new Linha("GET /itens", "PPPP", c -> get("/api/v1/itens")),
        new Linha("GET /itens/{id}", "PPPP", c -> get("/api/v1/itens/" + c.item())),
        new Linha("POST /itens", "PPPP", c -> json(post("/api/v1/itens"), "{\"codigo\":\"MTZ-"
            + UUID.randomUUID().toString().substring(0, 8) + "\",\"descricao\":\"Item da matriz\",\"unidade\":\"UN\","
            + "\"tipo\":\"MATERIAL\"}")),
        new Linha("PATCH /itens/{id}", "PPPP", c -> json(patch("/api/v1/itens/" + c.item()),
            "{\"quantidadeMinima\":3}")),

        new Linha("GET /canteiros/{id}/estoque", "PNPP", c -> get("/api/v1/canteiros/" + c.canteiroA() + "/estoque")),
        new Linha("GET /canteiros/{id}/estoque/{itemId}", "PNPP",
            c -> get("/api/v1/canteiros/" + c.canteiroA() + "/estoque/" + c.item())),
        new Linha("GET /canteiros/{id}/estoque/resumo", "PNPP",
            c -> get("/api/v1/canteiros/" + c.canteiroA() + "/estoque/resumo")),

        new Linha("POST /canteiros/{id}/movimentacoes", "PNNN", c -> json(
            post("/api/v1/canteiros/" + c.canteiroA() + "/movimentacoes"), "{\"id\":\"" + UUID.randomUUID()
                + "\",\"tipo\":\"ENTRADA\",\"motivo\":\"RECEBIMENTO\",\"itemId\":\"" + c.item() + "\",\"quantidade\":1}")),
        new Linha("GET /movimentacoes", "NNPP",
            c -> get("/api/v1/movimentacoes").param("canteiroId", c.canteiroA().toString())),
        new Linha("GET /movimentacoes/{id}", "PNPP", c -> get("/api/v1/movimentacoes/" + c.movimentacaoA())),
        new Linha("POST /movimentacoes/{id}/estorno", "PNNN", c -> json(
            post("/api/v1/movimentacoes/" + c.movimentacaoA() + "/estorno"),
            "{\"id\":\"" + UUID.randomUUID() + "\",\"motivo\":\"Teste da matriz\"}")),

        new Linha("POST /requisicoes", "PNPP", c -> json(post("/api/v1/requisicoes"), "{\"id\":\"" + UUID.randomUUID()
            + "\",\"canteiroId\":\"" + c.canteiroA() + "\",\"necessarioAte\":\"" + amanha()
            + "\",\"itens\":[{\"itemId\":\"" + c.item() + "\",\"quantidade\":2}]}")),
        new Linha("GET /requisicoes", "PNPP",
            c -> get("/api/v1/requisicoes").param("canteiroId", c.canteiroA().toString())),
        new Linha("GET /requisicoes/{id}", "PNPP", c -> get("/api/v1/requisicoes/" + c.requisicaoDoGestor())),
        new Linha("POST /requisicoes/{id}/aprovar (pedida pelo GESTOR)", "PNNN",
            c -> json(post("/api/v1/requisicoes/" + c.requisicaoDoGestor() + "/aprovar"), "{}")),
        new Linha("POST /requisicoes/{id}/aprovar (pedida pelo ALMOXARIFE)", "NNPP",
            c -> json(post("/api/v1/requisicoes/" + c.requisicaoDoAlmoxarife() + "/aprovar"), "{}")),
        new Linha("POST /requisicoes/{id}/recusar (pedida pelo ALMOXARIFE)", "NNPP",
            c -> json(post("/api/v1/requisicoes/" + c.requisicaoDoAlmoxarife() + "/recusar"),
                "{\"motivo\":\"Sem verba agora\"}")),
        new Linha("POST /requisicoes/{id}/atender", "PNNN",
            c -> post("/api/v1/requisicoes/" + c.requisicaoAprovada() + "/atender")),
        new Linha("POST /requisicoes/{id}/cancelar (pedida pelo GESTOR)", "NNPN",
            c -> post("/api/v1/requisicoes/" + c.requisicaoDoGestor() + "/cancelar")),

        new Linha("GET /auditoria", "NNPP", c -> get("/api/v1/auditoria")));

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
    private TransactionTemplate transacao;

    private Cenario cenario;

    @BeforeAll
    void prepararCenario() {
        Cenario base = transacao.execute(status -> {
            UUID canteiroA = dados.canteiro().getId();
            UUID canteiroB = dados.canteiro().getId();
            Usuario almoxarifeA = tokens.almoxarife(canteiroA);
            Usuario gestor = tokens.gestor();
            UUID item = dados.material().getId();
            return new Cenario(canteiroA, almoxarifeA, tokens.almoxarife(canteiroB), gestor, tokens.admin(),
                tokens.gestor(), item, dados.colaborador(canteiroA).getId(), null,
                requisicoes.pendente(gestor, canteiroA, item), requisicoes.pendente(almoxarifeA, canteiroA, item),
                requisicoes.gravar(gestor, canteiroA, "APROVADA", Instant.now(), item));
        });
        UUID movimentacao = movimentacoes.aplicarUmaVez(new NovaMovimentacao(UUID.randomUUID(), base.canteiroA(),
            base.item(), TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, new BigDecimal("10"), null, null,
            null, null, base.almoxarifeA().getId())).registro().getId();
        cenario = new Cenario(base.canteiroA(), base.almoxarifeA(), base.almoxarifeB(), base.gestor(), base.admin(),
            base.alvo(), base.item(), base.colaboradorA(), movimentacao, base.requisicaoDoGestor(),
            base.requisicaoDoAlmoxarife(), base.requisicaoAprovada());
    }

    static Stream<Arguments> casos() {
        List<Arguments> casos = new ArrayList<>();
        for (Linha linha : MATRIZ) {
            for (Perfil perfil : Perfil.values()) {
                casos.add(Arguments.of(linha, perfil, linha.esperado().charAt(perfil.coluna) == 'P'));
            }
        }
        return casos.stream();
    }

    @Nested
    @DisplayName("por perfil")
    class PorPerfil {

        @ParameterizedTest(name = "{0} como {1}: {2}")
        @MethodSource("br.com.canteiroconectado.security.MatrizDePermissoesTest#casos")
        @DisplayName("deve permitir ou negar cada endpoint conforme a matriz da história E05-H3")
        void deve_seguirAMatriz_quando_perfilChamaEndpoint(Linha linha, Perfil perfil, boolean permitido)
                throws Exception {
            MockHttpServletResponse resposta = mockMvc.perform(linha.pedido().apply(cenario)
                    .header("Authorization", tokens.bearer(cenario.usuario(perfil))))
                .andReturn().getResponse();

            if (permitido) {
                assertThat(resposta.getStatus())
                    .as("%s como %s deveria ser permitido: %s", linha, perfil, resposta.getContentAsString())
                    .isNotIn(401, 403)
                    .isLessThan(500);
            } else {
                assertThat(resposta.getStatus())
                    .as("%s como %s deveria ser negado: %s", linha, perfil, resposta.getContentAsString())
                    .isEqualTo(403);
                assertThat((String) JsonPath.read(resposta.getContentAsString(), "$.codigo"))
                    .isIn("ACESSO_NEGADO", "AUTOAPROVACAO_PROIBIDA");
            }
        }
    }

    @Nested
    @DisplayName("sem token")
    class SemToken {

        static Stream<Linha> linhas() {
            return MATRIZ.stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("linhas")
        @DisplayName("deve responder 401 NAO_AUTENTICADO em todo endpoint não público")
        void deve_responder401_quando_semToken(Linha linha) throws Exception {
            MockHttpServletResponse resposta = mockMvc.perform(linha.pedido().apply(cenario)).andReturn().getResponse();

            assertThat(resposta.getStatus()).as("%s sem token", linha).isEqualTo(401);
            assertThat((String) JsonPath.read(resposta.getContentAsString(), "$.codigo")).isEqualTo("NAO_AUTENTICADO");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("br.com.canteiroconectado.security.MatrizDePermissoesTest#publicos")
        @DisplayName("deve manter públicos o ping, o health e o Swagger")
        void deve_responder200_quando_publico(String caminho) throws Exception {
            assertThat(mockMvc.perform(get(caminho)).andReturn().getResponse().getStatus()).isEqualTo(200);
        }
    }

    static Stream<String> publicos() {
        return Stream.of("/api/v1/ping", "/actuator/health", "/v3/api-docs");
    }
}
