package br.com.canteiroconectado.service.estoque;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.dominio.estoque.SaldoEstoque;
import br.com.canteiroconectado.dominio.estoque.StatusEstoque;
import br.com.canteiroconectado.dominio.item.TipoItem;
import br.com.canteiroconectado.dto.response.AlertasEstoqueResponse;
import br.com.canteiroconectado.dto.response.DetalharEstoqueResponse;
import br.com.canteiroconectado.dto.response.ItemEstoqueResponse;
import br.com.canteiroconectado.dto.response.ItemResponse;
import br.com.canteiroconectado.dto.response.PaginaResponse;
import br.com.canteiroconectado.repository.estoque.EstoqueConsultaRepository;
import br.com.canteiroconectado.repository.estoque.MovimentacaoConsultaRepository;
import br.com.canteiroconectado.repository.estoque.SaldoEstoqueRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.service.canteiro.CanteiroService;
import br.com.canteiroconectado.service.item.ItemService;
import br.com.canteiroconectado.util.Paginacao;
import br.com.canteiroconectado.util.Quantidades;

@Service
public class EstoqueService {

    static final int ULTIMAS_MOVIMENTACOES = 10;

    private final EstoqueConsultaRepository consultas;
    private final MovimentacaoConsultaRepository movimentacoes;
    private final SaldoEstoqueRepository saldos;
    private final CanteiroService canteiros;
    private final ItemService itens;

    public EstoqueService(EstoqueConsultaRepository consultas, MovimentacaoConsultaRepository movimentacoes,
            SaldoEstoqueRepository saldos, CanteiroService canteiros, ItemService itens) {
        this.consultas = consultas;
        this.movimentacoes = movimentacoes;
        this.saldos = saldos;
        this.canteiros = canteiros;
        this.itens = itens;
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public AlertasEstoqueResponse alertas(UUID canteiroId, int quantosCriticos) {
        canteiros.buscar(canteiroId);
        Map<StatusEstoque, Long> contagem = consultas.contarPorStatus(canteiroId);
        return new AlertasEstoqueResponse(contagem.get(StatusEstoque.ZERADO), contagem.get(StatusEstoque.ABAIXO_MINIMO),
            consultas.maisCriticos(canteiroId, quantosCriticos));
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public DetalharEstoqueResponse detalhar(UUID canteiroId, UUID itemId) {
        canteiros.buscar(canteiroId);
        ItemResponse item = itens.buscar(itemId);
        BigDecimal saldo = saldos.buscar(canteiroId, itemId).map(SaldoEstoque::getQuantidade).orElse(BigDecimal.ZERO);
        return new DetalharEstoqueResponse(item.id(), item.codigo(), item.descricao(), item.unidade(),
            item.unidadeSimbolo(), item.tipo(), item.ativo(), item.quantidadeMinima(), Quantidades.normalizar(saldo),
            StatusEstoque.of(saldo, item.quantidadeMinima()),
            movimentacoes.ultimas(canteiroId, itemId, ULTIMAS_MOVIMENTACOES));
    }

    @PreAuthorize(Permissoes.AUTENTICADO)
    @Transactional(readOnly = true)
    public PaginaResponse<ItemEstoqueResponse> listar(UUID canteiroId, String busca, List<StatusEstoque> status,
            TipoItem tipo, Integer pagina, Integer tamanho) {
        canteiros.buscar(canteiroId);
        Pageable paginacao = Paginacao.of(pagina, tamanho);
        String termo = Paginacao.termoBusca(busca);
        Set<StatusEstoque> filtro = status == null || status.isEmpty()
            ? EnumSet.allOf(StatusEstoque.class)
            : EnumSet.copyOf(status);

        long total = consultas.contar(canteiroId, termo, tipo, filtro);
        List<ItemEstoqueResponse> linhas = consultas.listar(canteiroId, termo, tipo, filtro,
            paginacao.getPageSize(), paginacao.getOffset());
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(linhas, paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }
}
