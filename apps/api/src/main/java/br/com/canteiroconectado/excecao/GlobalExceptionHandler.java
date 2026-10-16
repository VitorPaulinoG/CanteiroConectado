package br.com.canteiroconectado.excecao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.canteiroconectado.filtro.RequestIdFilter;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException excecao) {
        List<String> campos = excecao.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .toList();
        ProblemDetail problema = problemaBase(HttpStatus.BAD_REQUEST, ErrorCode.VALIDACAO_INVALIDA,
            "Um ou mais campos são inválidos.");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleRegraNegocio(BusinessException excecao) {
        return problemaBase(excecao.status(), excecao.codigo(), excecao.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAcessoNegado(AccessDeniedException excecao) {
        return problemaBase(HttpStatus.FORBIDDEN, ErrorCode.ACESSO_NEGADO,
            "Você não tem permissão para esta ação.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleNaoAutenticado(AuthenticationException excecao) {
        return problemaBase(HttpStatus.UNAUTHORIZED, ErrorCode.NAO_AUTENTICADO,
            "É necessário autenticar-se.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErroInesperado(Exception excecao) {
        LOG.error("Erro inesperado [requestId={}]", MDC.get(RequestIdFilter.MDC_KEY), excecao);
        return problemaBase(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERRO_INESPERADO,
            "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private static ProblemDetail problemaBase(HttpStatus status, ErrorCode codigo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setProperty("codigo", codigo.name());
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problema.setProperty("requestId", requestId);
        }
        return problema;
    }
}
