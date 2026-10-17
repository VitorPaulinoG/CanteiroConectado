package br.com.canteiroconectado.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerFalso())
            .setControllerAdvice(handler)
            .build();
    }

    @Nested
    @DisplayName("handleValidacao")
    class HandleValidacao {

        @Test
        @DisplayName("deve devolver 400 com a lista de campos quando o corpo tem campo inválido")
        void deve_devolver400ComCampos_quando_corpoInvalido() throws Exception {
            mockMvc.perform(post("/falso/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.campos[0]").value(org.hamcrest.Matchers.startsWith("nome:")));
        }
    }

    @Nested
    @DisplayName("handleRegraNegocio")
    class HandleRegraNegocio {

        @Test
        @DisplayName("deve devolver o status e o código da exceção quando é uma regra de negócio")
        void deve_devolverStatusECodigo_quando_regraNegocioFalha() throws Exception {
            mockMvc.perform(get("/falso/negocio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.detail").value("Regra violada"));
        }
    }

    @Nested
    @DisplayName("handleRequisicaoInvalida")
    class HandleRequisicaoInvalida {

        @Test
        @DisplayName("deve devolver 400 quando o corpo da requisição é ilegível")
        void deve_devolver400_quando_corpoIlegivel() throws Exception {
            mockMvc.perform(post("/falso/validar").contentType(MediaType.APPLICATION_JSON).content("{nao-e-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("handleNaoEncontrado")
    class HandleNaoEncontrado {

        @Test
        @DisplayName("deve devolver 404 com o código de recurso não encontrado quando o endereço não existe")
        void deve_devolver404_quando_enderecoInexistente() {
            ProblemDetail problema = handler.handleNaoEncontrado(
                new NoResourceFoundException(HttpMethod.GET, "/falso/inexistente", "/falso/inexistente"));

            assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(problema.getProperties()).containsEntry("codigo", "RECURSO_NAO_ENCONTRADO");
        }
    }

    @Nested
    @DisplayName("handleMetodoNaoPermitido")
    class HandleMetodoNaoPermitido {

        @Test
        @DisplayName("deve devolver 405 quando o método HTTP não existe para o endereço")
        void deve_devolver405_quando_metodoNaoPermitido() throws Exception {
            mockMvc.perform(post("/falso/negocio"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NAO_PERMITIDO"));
        }

        @Test
        @DisplayName("deve usar o código de método não permitido quando chamado diretamente")
        void deve_usarCodigo_quando_chamadoDiretamente() {
            ProblemDetail problema = handler.handleMetodoNaoPermitido(
                new HttpRequestMethodNotSupportedException("DELETE"));

            assertThat(problema.getProperties()).containsEntry("codigo", "METODO_NAO_PERMITIDO");
        }
    }

    @Nested
    @DisplayName("handleConflito")
    class HandleConflito {

        @Test
        @DisplayName("deve devolver 409 quando o banco rejeita por integridade")
        void deve_devolver409_quando_integridadeViolada() {
            ProblemDetail problema = handler.handleConflito(new DataIntegrityViolationException("duplicado"));

            assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(problema.getProperties()).containsEntry("codigo", "CONFLITO");
        }
    }

    @Nested
    @DisplayName("handleErroInesperado")
    class HandleErroInesperado {

        @Test
        @DisplayName("deve devolver 500 genérico sem detalhe da exceção quando ocorre um erro inesperado")
        void erroInesperadoDevolve500SemDetalheExcecao() throws Exception {
            mockMvc.perform(get("/falso/erro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INESPERADO"))
                .andExpect(jsonPath("$.detail").value(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo-interno"))));
        }
    }

    @RestController
    static class ControllerFalso {

        record Corpo(@NotBlank String nome) { }

        @PostMapping("/falso/validar")
        void validar(@Valid @RequestBody Corpo corpo) { }

        @GetMapping("/falso/negocio")
        void negocio() {
            throw new BusinessException(ErrorCode.CONFLITO, "Regra violada");
        }

        @GetMapping("/falso/erro")
        void erro() {
            throw new IllegalStateException("segredo-interno");
        }
    }
}
