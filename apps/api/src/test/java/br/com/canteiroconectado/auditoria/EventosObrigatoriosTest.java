package br.com.canteiroconectado.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

/**
 * Percorre pela API cada ação sensível da história E11-H3 e confere que todas viraram evento em
 * {@code log_auditoria} para o registro certo. Falha se uma ação de
 * {@link AcaoAuditoria#OBRIGATORIAS} deixar de ser gravada ou não estiver coberta aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("Eventos obrigatórios da trilha de auditoria")
class EventosObrigatoriosTest {

    private static final String NOVA_SENHA = "senha-propria-9";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private LogAuditoriaRepository logs;

    private final Map<AcaoAuditoria, UUID> esperados = new EnumMap<>(AcaoAuditoria.class);

    private String executar(MockHttpServletRequestBuilder pedido, Usuario autor, String corpo, int esperado)
            throws Exception {
        if (autor != null) {
            pedido.header("Authorization", tokens.bearer(autor));
        }
        if (corpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(pedido).andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString();
    }

    private String entrar(String email, String senha, int esperado) throws Exception {
        return executar(post("/api/v1/autenticacao/login"), null,
            "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", esperado);
    }

    private static UUID id(String json) {
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Nested
    @DisplayName("fluxo completo")
    class FluxoCompleto {

        @Test
        @DisplayName("deve gravar cada evento obrigatório para o registro afetado, sem senha nos detalhes")
        void deve_gravarTodosOsEventos_quando_acoesSensiveisAcontecem() throws Exception {
            Usuario admin = tokens.admin();

            UUID canteiro = id(executar(post("/api/v1/canteiros"), admin, "{\"nome\":\"Obra Auditada " + unico() + "\"}",
                201));
            esperados.put(AcaoAuditoria.CANTEIRO_CRIADO, canteiro);
            executar(patch("/api/v1/canteiros/" + canteiro), admin, "{\"endereco\":\"Rua Nova, 1\"}", 200);
            esperados.put(AcaoAuditoria.CANTEIRO_EDITADO, canteiro);

            String email = "auditado." + unico() + "@obra.com";
            UUID funcionario = id(executar(post("/api/v1/usuarios"), admin, "{\"nome\":\"Auditado\",\"email\":\"" + email
                + "\",\"perfil\":\"ALMOXARIFE\",\"canteiroId\":\"" + canteiro + "\"}", 201));
            esperados.put(AcaoAuditoria.USUARIO_CRIADO, funcionario);
            String temporaria = JsonPath.read(executar(get("/api/v1/usuarios/" + funcionario + "/credencial-inicial"),
                admin, null, 200), "$.senhaTemporaria");
            esperados.put(AcaoAuditoria.CREDENCIAL_CONSULTADA, funcionario);

            entrar(email, "senha-errada-1", 401);
            esperados.put(AcaoAuditoria.LOGIN_FALHOU, funcionario);
            String token = JsonPath.read(entrar(email, temporaria, 200), "$.token");
            esperados.put(AcaoAuditoria.LOGIN_SUCESSO, funcionario);
            mockMvc.perform(post("/api/v1/autenticacao/trocar-senha")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"senhaAtual\":\"" + temporaria + "\",\"novaSenha\":\"" + NOVA_SENHA + "\"}"))
                .andExpect(status().isOk());
            esperados.put(AcaoAuditoria.SENHA_TROCADA, funcionario);
            executar(post("/api/v1/usuarios/" + funcionario + "/redefinir-senha"), admin, null, 200);
            esperados.put(AcaoAuditoria.SENHA_REDEFINIDA, funcionario);

            Usuario bloqueado = tokens.gestor();
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                entrar(bloqueado.getEmail(), "senha-errada-1", tentativa < 4 ? 401 : 423);
            }
            esperados.put(AcaoAuditoria.CONTA_BLOQUEADA, bloqueado.getId());

            Usuario almoxarife = tokens.almoxarife(canteiro);
            UUID item = id(executar(post("/api/v1/itens"), almoxarife, "{\"codigo\":\"AUD-" + unico()
                + "\",\"descricao\":\"Item auditado\",\"unidade\":\"UN\",\"tipo\":\"MATERIAL\"}", 201));
            esperados.put(AcaoAuditoria.ITEM_CRIADO, item);
            executar(patch("/api/v1/itens/" + item), almoxarife, "{\"quantidadeMinima\":5}", 200);
            esperados.put(AcaoAuditoria.ITEM_EDITADO, item);

            UUID entrada = id(executar(post("/api/v1/canteiros/" + canteiro + "/movimentacoes"), almoxarife,
                "{\"id\":\"" + UUID.randomUUID() + "\",\"tipo\":\"ENTRADA\",\"motivo\":\"RECEBIMENTO\",\"itemId\":\""
                    + item + "\",\"quantidade\":10}", 201));
            executar(post("/api/v1/movimentacoes/" + entrada + "/estorno"), almoxarife,
                "{\"id\":\"" + UUID.randomUUID() + "\",\"motivo\":\"Nota duplicada\"}", 201);
            esperados.put(AcaoAuditoria.MOVIMENTACAO_ESTORNADA, entrada);

            Usuario camila = tokens.gestor();
            String pedido = "\",\"canteiroId\":\"" + canteiro + "\",\"necessarioAte\":\""
                + LocalDate.now(Fuso.DAS_OBRAS).plusDays(3) + "\",\"itens\":[{\"itemId\":\"" + item
                + "\",\"quantidade\":5}]}";
            UUID aprovada = id(executar(post("/api/v1/requisicoes"), camila, "{\"id\":\"" + UUID.randomUUID() + pedido,
                201));
            UUID recusada = id(executar(post("/api/v1/requisicoes"), camila, "{\"id\":\"" + UUID.randomUUID() + pedido,
                201));
            executar(post("/api/v1/requisicoes/" + aprovada + "/aprovar"), almoxarife, "{}", 200);
            esperados.put(AcaoAuditoria.REQUISICAO_APROVADA, aprovada);
            executar(post("/api/v1/requisicoes/" + recusada + "/recusar"), almoxarife,
                "{\"motivo\":\"Sem verba agora\"}", 200);
            esperados.put(AcaoAuditoria.REQUISICAO_RECUSADA, recusada);

            executar(post("/api/v1/usuarios/" + funcionario + "/desativar"), admin, null, 200);
            esperados.put(AcaoAuditoria.USUARIO_DESATIVADO, funcionario);

            List<LogAuditoria> todos = logs.findAll();
            assertThat(todos).anyMatch(log -> log.getAcao() == AcaoAuditoria.ADMIN_CRIADO);
            for (Map.Entry<AcaoAuditoria, UUID> esperado : esperados.entrySet()) {
                assertThat(todos)
                    .as("evento %s para %s", esperado.getKey(), esperado.getValue())
                    .anyMatch(log -> log.getAcao() == esperado.getKey()
                        && esperado.getValue().equals(log.getEntidadeId()));
            }
            assertThat(esperados.keySet()).as("ações obrigatórias cobertas por este teste")
                .containsAll(AcaoAuditoria.OBRIGATORIAS.stream()
                    .filter(acao -> acao != AcaoAuditoria.ADMIN_CRIADO)
                    .toList());
            assertThat(todos).extracting(log -> String.valueOf(log.getDetalhes()))
                .noneMatch(detalhes -> detalhes.contains(temporaria) || detalhes.contains(NOVA_SENHA)
                    || detalhes.contains(Tokens.SENHA) || detalhes.contains(token));
        }
    }
}
