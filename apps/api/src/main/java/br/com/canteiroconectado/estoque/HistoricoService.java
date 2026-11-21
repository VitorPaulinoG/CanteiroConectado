package br.com.canteiroconectado.estoque;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;

/**
 * Histórico completo de movimentações do canteiro, só para o GESTOR. Somente leitura: o livro não
 * oferece editar nem excluir; correção é por estorno.
 */
@Service
public class HistoricoService {

    private final MovimentacaoConsultaRepository consultas;
    private final CanteiroAcessoService acesso;

    public HistoricoService(MovimentacaoConsultaRepository consultas, CanteiroAcessoService acesso) {
        this.consultas = consultas;
        this.acesso = acesso;
    }

    /**
     * @param de começo do período, inclusive (UTC)
     * @param ate fim do período, exclusive (UTC)
     * @throws BusinessException {@code VALIDACAO_INVALIDA} sem canteiro ou com {@code de} depois de
     *     {@code ate}
     */
    @PreAuthorize(Permissoes.GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoResponse> listar(UUID canteiroId, Instant de, Instant ate, TipoMovimentacao tipo,
            UUID itemId, UUID colaboradorId, Integer pagina, Integer tamanho) {
        if (de != null && ate != null && !de.isBefore(ate)) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "O começo do período deve ser antes do fim.")
                .com("campo", "de");
        }
        FiltroDoHistorico filtro = new FiltroDoHistorico(acesso.resolver(canteiroId), de, ate, tipo, itemId,
            colaboradorId);
        Pageable paginacao = Paginacao.de(pagina, tamanho);
        long total = consultas.contarHistorico(filtro);
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(consultas.historico(filtro, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }
}
