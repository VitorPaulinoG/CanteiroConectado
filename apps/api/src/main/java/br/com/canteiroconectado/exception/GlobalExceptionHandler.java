package br.com.canteiroconectado.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.canteiroconectado.filter.RequestIdFilter;

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

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class,
        ConstraintViolationException.class})
    public ProblemDetail handleRequisicaoInvalida(Exception excecao) {
        return problemaBase(HttpStatus.BAD_REQUEST, ErrorCode.VALIDACAO_INVALIDA,
            "A requisição é inválida: confira o corpo e os parâmetros enviados.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNaoEncontrado(NoResourceFoundException excecao) {
        return problemaBase(HttpStatus.NOT_FOUND, ErrorCode.RECURSO_NAO_ENCONTRADO,
            "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMetodoNaoPermitido(HttpRequestMethodNotSupportedException excecao) {
        return problemaBase(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METODO_NAO_PERMITIDO,
            "Método HTTP não permitido para este endereço.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConflito(DataIntegrityViolationException excecao) {
        LOG.warn("Conflito de integridade [requestId={}]", MDC.get(RequestIdFilter.MDC_KEY), excecao);
        return problemaBase(HttpStatus.CONFLICT, ErrorCode.CONFLITO,
            "A operação conflita com o estado atual dos dados.");
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
