package br.com.canteiroconectado.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sobe a API com o perfil {@code prod}, com valores de teste no lugar das variáveis obrigatórias,
 * e confere o que a produção esconde. O SSL do banco é desligado só aqui, porque o Postgres dos
 * testes roda sem certificado.
 */
@SpringBootTest(properties = {
    "canteiro.banco.url=${BANCO_URL:jdbc:postgresql://localhost:5432/canteiroconectado}",
    "canteiro.banco.usuario=${BANCO_USUARIO:canteiro}",
    "canteiro.banco.senha=${BANCO_SENHA:canteiro}",
    "canteiro.jwt.segredo=segredo-de-teste-do-perfil-de-producao-com-mais-de-32-bytes",
    "canteiro.admin.nome=Administrador Local",
    "canteiro.admin.email=admin@canteiro.local",
    "canteiro.admin.senha=admin-local-123",
    "canteiro.senha-temporaria.chave=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "spring.datasource.hikari.data-source-properties.sslmode=disable"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@DisplayName("Perfil de produção")
class PerfilDeProducaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("deve desligar o Swagger e a especificação OpenAPI")
    void deve_desligarSwagger_quando_perfilProd() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deve responder ao health sem detalhes do banco nem dos componentes")
    void deve_esconderDetalhesDoHealth_quando_perfilProd() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("deve recusar o JSON quebrado sem repetir a mensagem do Jackson")
    void deve_esconderDetalheInterno_quando_corpoIlegivel() throws Exception {
        mockMvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(content().string(not(containsString("Jackson"))))
            .andExpect(content().string(not(containsString("JSON parse error"))));
    }
}
