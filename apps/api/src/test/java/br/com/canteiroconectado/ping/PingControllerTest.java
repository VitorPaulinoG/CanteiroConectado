package br.com.canteiroconectado.ping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PingController")
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/ping")
    class Ping {

        @Test
        @DisplayName("deve devolver nome, versão e hora do servidor em UTC sem exigir token")
        void deve_devolverNomeVersaoEHora_quando_chamadoSemToken() throws Exception {
            mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("canteiroconectado"))
                .andExpect(jsonPath("$.versao").isNotEmpty())
                .andExpect(jsonPath("$.horaServidor").value(org.hamcrest.Matchers.endsWith("Z")));
        }
    }
}
