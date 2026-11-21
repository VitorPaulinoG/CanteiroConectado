package br.com.canteiroconectado.service.estoque;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.dominio.estoque.HistoricoQuery;
import br.com.canteiroconectado.dominio.estoque.TipoMovimentacao;
import br.com.canteiroconectado.dto.response.MovimentacaoResponse;
import br.com.canteiroconectado.dto.response.PaginaResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.estoque.MovimentacaoConsultaRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.service.canteiro.CanteiroAcessoService;
import br.com.canteiroconectado.util.Paginacao;

@Service
public class HistoricoService {

    private final MovimentacaoConsultaRepository consultas;
    private final CanteiroAcessoService acesso;

    public HistoricoService(MovimentacaoConsultaRepository consultas, CanteiroAcessoService acesso) {
        this.consultas = consultas;
        this.acesso = acesso;
    }

    @PreAuthorize(Permissoes.GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoResponse> listar(UUID canteiroId, Instant inicio, Instant ate, TipoMovimentacao tipo,
            UUID itemId, UUID colaboradorId, Integer pagina, Integer tamanho) {
        if (inicio != null && ate != null && !inicio.isBefore(ate)) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "O começo do período deve ser antes do fim.")
                .com("campo", "de");
        }
        HistoricoQuery filtro = new HistoricoQuery(acesso.resolver(canteiroId), inicio, ate, tipo, itemId,
            colaboradorId);
        Pageable paginacao = Paginacao.of(pagina, tamanho);
        long total = consultas.contarHistorico(filtro);
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(consultas.historico(filtro, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }
}
