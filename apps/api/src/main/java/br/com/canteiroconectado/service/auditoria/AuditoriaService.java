package br.com.canteiroconectado.service.auditoria;

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

import br.com.canteiroconectado.dominio.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.dominio.auditoria.LogAuditoria;
import br.com.canteiroconectado.dto.response.EventoAuditoriaResponse;
import br.com.canteiroconectado.dto.response.PaginaResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.filter.RequestIdFilter;
import br.com.canteiroconectado.repository.auditoria.AuditoriaConsultaRepository;
import br.com.canteiroconectado.repository.auditoria.LogAuditoriaRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.util.Paginacao;

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

    @PreAuthorize(Permissoes.GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<EventoAuditoriaResponse> listar(UUID usuarioId, AcaoAuditoria acao, Instant inicio,
            Instant ate, Integer pagina, Integer tamanho) {
        if (inicio != null && ate != null && !inicio.isBefore(ate)) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "O começo do período deve ser antes do fim.")
                .com("campo", "inicio");
        }
        AuditoriaConsultaRepository.Filtro filtro = new AuditoriaConsultaRepository.Filtro(usuarioId, acao, inicio, ate);
        Pageable paginacao = Paginacao.of(pagina, tamanho);
        long total = consultas.contar(filtro);
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(consultas.listar(filtro, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public LogAuditoria registrar(UUID usuarioId, AcaoAuditoria acao, String entidade, UUID entidadeId,
            Map<String, Object> detalhes) {
        return logs.save(new LogAuditoria(usuarioId, acao, entidade, entidadeId, detalhes,
            MDC.get(RequestIdFilter.MDC_KEY), OffsetDateTime.now(relogio)));
    }
}
