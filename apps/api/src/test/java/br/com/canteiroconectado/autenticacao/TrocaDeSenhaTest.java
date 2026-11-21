package br.com.canteiroconectado.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoria;
import br.com.canteiroconectado.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("Troca de senha e primeiro acesso")
class TrocaDeSenhaTest {

    private static final String NOVA_SENHA = "nova-senha-9";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private LogAuditoriaRepository logs;

    @Autowired
    private PasswordEncoder encoderSenha;

    private Usuario funcionario;

    @BeforeEach
    void criarFuncionarioComSenhaTemporaria() {
        funcionario = tokens.gestor();
        funcionario.setPrecisaTrocarSenha(true);
        funcionario.setSenhaTemporariaCifrada("cifrada");
        usuarios.saveAndFlush(funcionario);
    }

    private ResultActions trocar(Usuario usuario, String atual, String nova) throws Exception {
        return mockMvc.perform(post("/api/v1/autenticacao/trocar-senha")
            .header("Authorization", tokens.bearer(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"senhaAtual\":\"" + atual + "\",\"novaSenha\":\"" + nova + "\"}"));
    }

    @Nested
    @DisplayName("POST /api/v1/autenticacao/trocar-senha")
    class TrocarSenha {

        @Test
        @DisplayName("deve trocar a senha, apagar a temporária e registrar SENHA_TROCADA sem senha")
        void deve_trocarSenha_quando_regrasAtendidas() throws Exception {
            trocar(funcionario, Tokens.SENHA, NOVA_SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precisaTrocarSenha").value(false));

            Usuario salvo = usuarios.findById(funcionario.getId()).orElseThrow();
            assertThat(salvo.isPrecisaTrocarSenha()).isFalse();
            assertThat(salvo.getSenhaTemporariaCifrada()).isNull();
            assertThat(encoderSenha.matches(NOVA_SENHA, salvo.getSenhaHash())).isTrue();
            assertThat(logs.findByEntidadeIdOrderByCriadoEmAsc(funcionario.getId()))
                .filteredOn(log -> log.getAcao() == AcaoAuditoria.SENHA_TROCADA)
                .singleElement()
                .extracting(LogAuditoria::getDetalhes)
                .asString()
                .doesNotContain(NOVA_SENHA, Tokens.SENHA);
        }

        @Test
        @DisplayName("deve recusar com SENHA_ATUAL_INCORRETA quando a senha atual não confere")
        void deve_recusar_quando_senhaAtualErrada() throws Exception {
            trocar(funcionario, "outra-senha-1", NOVA_SENHA)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SENHA_ATUAL_INCORRETA"));
        }

        @Test
        @DisplayName("deve recusar com SENHA_FRACA e o motivo quando a nova senha não tem número")
        void deve_recusar_quando_senhaFraca() throws Exception {
            trocar(funcionario, Tokens.SENHA, "somenteletras")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SENHA_FRACA"))
                .andExpect(jsonPath("$.detail").value("A senha deve ter pelo menos um número."));
        }

        @Test
        @DisplayName("deve recusar com SENHA_IGUAL_A_ATUAL quando a nova senha é a mesma")
        void deve_recusar_quando_senhaIgualAtual() throws Exception {
            trocar(funcionario, Tokens.SENHA, Tokens.SENHA)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SENHA_IGUAL_A_ATUAL"));
        }
    }

    @Nested
    @DisplayName("troca obrigatória")
    class TrocaObrigatoria {

        @Test
        @DisplayName("deve liberar só /eu e /trocar-senha para funcionário com troca pendente")
        void deve_bloquearDemais_quando_trocaPendente() throws Exception {
            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", tokens.bearer(funcionario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precisaTrocarSenha").value(true));
            mockMvc.perform(get("/api/v1/qualquer-recurso").header("Authorization", tokens.bearer(funcionario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TROCA_DE_SENHA_OBRIGATORIA"));
        }

        @Test
        @DisplayName("deve liberar o resto depois da troca")
        void deve_liberar_quando_senhaTrocada() throws Exception {
            trocar(funcionario, Tokens.SENHA, NOVA_SENHA).andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/qualquer-recurso").header("Authorization", tokens.bearer(funcionario)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("não deve bloquear o administrador, para quem a troca é opcional")
        void naoDeve_bloquear_quando_administrador() throws Exception {
            Usuario admin = tokens.admin();
            admin.setPrecisaTrocarSenha(true);
            usuarios.saveAndFlush(admin);

            mockMvc.perform(get("/api/v1/qualquer-recurso").header("Authorization", tokens.bearer(admin)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("usuário desativado")
    class UsuarioDesativado {

        @Test
        @DisplayName("deve devolver 401 quando o token é de um usuário desativado")
        void deve_devolver401_quando_usuarioDesativado() throws Exception {
            String token = tokens.bearer(funcionario);
            funcionario.setAtivo(false);
            usuarios.saveAndFlush(funcionario);

            mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        }
    }
}
