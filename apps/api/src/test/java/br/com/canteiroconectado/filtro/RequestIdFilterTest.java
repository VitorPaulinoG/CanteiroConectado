package br.com.canteiroconectado.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RequestIdFilter")
class RequestIdFilterTest {

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capturarLogAcesso() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger("acesso")).addAppender(appender);
    }

    @AfterEach
    void limparAppender() {
        ((Logger) LoggerFactory.getLogger("acesso")).detachAppender(appender);
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        private final RequestIdFilter filtro = new RequestIdFilter();

        @Test
        @DisplayName("deve gerar um requestId novo quando a requisição não traz o cabeçalho")
        void deve_gerarRequestId_quando_cabecalhoAusente() throws Exception {
            MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/api/v1/ping");
            MockHttpServletResponse resposta = new MockHttpServletResponse();

            filtro.doFilter(requisicao, resposta,
                (req, res) -> assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNotBlank());

            assertThat(resposta.getHeader(RequestIdFilter.HEADER)).isNotBlank();
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("deve reaproveitar o requestId quando a requisição já traz o cabeçalho")
        void deve_reaproveitarRequestId_quando_cabecalhoPresente() throws Exception {
            MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/api/v1/ping");
            requisicao.addHeader(RequestIdFilter.HEADER, "id-recebido-123");
            MockHttpServletResponse resposta = new MockHttpServletResponse();

            filtro.doFilter(requisicao, resposta, (req, res) -> { });

            assertThat(resposta.getHeader(RequestIdFilter.HEADER)).isEqualTo("id-recebido-123");
        }

        @Test
        @DisplayName("deve omitir autorização e senha do log quando grava a linha de acesso")
        void deve_omitirAutorizacaoESenha_quando_gravaLogAcesso() throws Exception {
            MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", "/api/v1/autenticacao/login");
            requisicao.addHeader("Authorization", "Bearer token-super-secreto");
            MockHttpServletResponse resposta = new MockHttpServletResponse();
            resposta.setStatus(200);

            filtro.doFilter(requisicao, resposta, (req, res) -> { });

            assertThat(appender.list).hasSize(1);
            String mensagem = appender.list.get(0).getFormattedMessage();
            assertThat(mensagem).doesNotContain("token-super-secreto", "Authorization", "Bearer");
            assertThat(mensagem).contains("POST", "/api/v1/autenticacao/login", "200");
        }
    }
}
