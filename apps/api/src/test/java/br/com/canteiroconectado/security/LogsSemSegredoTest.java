package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import br.com.canteiroconectado.support.Tokens;
import br.com.canteiroconectado.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(Tokens.class)
@DisplayName("Logs sem segredo")
class LogsSemSegredoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tokens tokens;

    private final Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private ListAppender<ILoggingEvent> eventos;
    private Level nivelAnterior;

    @BeforeEach
    void capturar() {
        eventos = new ListAppender<>();
        eventos.start();
        raiz.addAppender(eventos);
        nivelAnterior = raiz.getLevel();
        raiz.setLevel(Level.DEBUG);
    }

    @AfterEach
    void soltar() {
        raiz.detachAppender(eventos);
        raiz.setLevel(nivelAnterior);
    }

    private String login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("não deve registrar a senha nem o token em nenhum log, mesmo em DEBUG, no login certo, errado e no uso do token")
    void naoDeve_registrarSenhaNemToken_quando_loginEUsoDoToken() throws Exception {
        Usuario usuario = tokens.almoxarife();
        String senhaErrada = "senha-errada-que-nao-pode-vazar-7";

        login(usuario.getEmail(), senhaErrada);
        String token = JsonPath.read(login(usuario.getEmail(), Tokens.SENHA), "$.token");
        mockMvc.perform(get("/api/v1/autenticacao/eu").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        assertThat(eventos.list).isNotEmpty();
        assertThat(eventos.list).extracting(ILoggingEvent::getFormattedMessage)
            .noneMatch(mensagem -> mensagem.contains(senhaErrada))
            .noneMatch(mensagem -> mensagem.contains(Tokens.SENHA))
            .noneMatch(mensagem -> mensagem.contains(token));
    }
}
