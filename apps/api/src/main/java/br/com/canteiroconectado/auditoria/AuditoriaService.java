package br.com.canteiroconectado.auditoria;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.filter.RequestIdFilter;
import br.com.canteiroconectado.security.Permissoes;

/**
 * Grava e consulta os eventos da trilha de auditoria. O {@code requestId} vem do MDC preenchido pelo
 * {@link RequestIdFilter}, para ligar o evento às linhas de log da mesma requisição.
 */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository logs;
    private final AuditoriaConsultaRepository consultas;
    private final Clock relogio;

    public AuditoriaService(LogAuditoriaRepository logs, AuditoriaConsultaRepository consultas, Clock relogio) {
        this.logs = logs;
        this.consultas = consultas;
        this.relogio = relogio;
    }

    /**
     * Trilha de auditoria para o GESTOR, somente leitura, do evento mais recente para o mais
     * antigo.
     *
     * @param de começo do período, inclusive (UTC)
     * @param ate fim do período, exclusive (UTC)
     */
    @PreAuthorize(Permissoes.GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<EventoDeAuditoriaResponse> listar(UUID usuarioId, AcaoAuditoria acao, Instant de,
            Instant ate, Integer pagina, Integer tamanho) {
        if (de != null && ate != null && !de.isBefore(ate)) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "O começo do período deve ser antes do fim.")
                .com("campo", "de");
        }
        AuditoriaConsultaRepository.Filtro filtro = new AuditoriaConsultaRepository.Filtro(usuarioId, acao, de, ate);
        Pageable paginacao = Paginacao.de(pagina, tamanho);
        long total = consultas.contar(filtro);
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(consultas.listar(filtro, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }

    /**
     * Registra um evento na transação corrente: se a operação auditada for desfeita, o evento
     * também é.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LogAuditoria registrar(UUID usuarioId, AcaoAuditoria acao, String entidade, UUID entidadeId,
            Map<String, Object> detalhes) {
        return logs.save(new LogAuditoria(usuarioId, acao, entidade, entidadeId, detalhes,
            MDC.get(RequestIdFilter.MDC_KEY), OffsetDateTime.now(relogio)));
    }
}
