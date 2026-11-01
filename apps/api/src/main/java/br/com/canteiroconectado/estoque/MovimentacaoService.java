package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.item.ItemService;

/**
 * Núcleo transacional do estoque: aplica uma movimentação ao livro e ao saldo na mesma transação.
 *
 * <p>A linha de saldo do item no canteiro é criada na primeira movimentação e lida com bloqueio
 * de escrita; assim, duas saídas simultâneas do mesmo item são aplicadas uma depois da outra e o
 * saldo nunca fica negativo. Esta classe não confere permissão: quem a chama (registro pelo app,
 * estorno, carga de demonstração) já conferiu perfil e canteiro.</p>
 */
@Service
public class MovimentacaoService {

    static final int CASAS_DECIMAIS = 3;

    private final MovimentacaoEstoqueRepository movimentacoes;
    private final SaldoEstoqueRepository saldos;
    private final CanteiroService canteiros;
    private final ItemService itens;
    private final Clock relogio;

    public MovimentacaoService(MovimentacaoEstoqueRepository movimentacoes, SaldoEstoqueRepository saldos,
            CanteiroService canteiros, ItemService itens, Clock relogio) {
        this.movimentacoes = movimentacoes;
        this.saldos = saldos;
        this.canteiros = canteiros;
        this.itens = itens;
        this.relogio = relogio;
    }

    /**
     * Valida, bloqueia o saldo, grava a movimentação com {@code saldo_apos} e atualiza o saldo.
     *
     * @throws BusinessException {@code QUANTIDADE_INVALIDA}, {@code CANTEIRO_INATIVO},
     *     {@code ITEM_INATIVO} ou {@code SALDO_INSUFICIENTE} (com {@code saldoAtual})
     */
    @Transactional
    public MovimentacaoEstoque aplicar(NovaMovimentacao nova) {
        BigDecimal quantidade = validarQuantidade(nova.quantidade());
        canteiros.exigirAtivo(nova.canteiroId());
        itens.exigirAtivo(nova.itemId());

        saldos.criarSeAusente(nova.canteiroId(), nova.itemId());
        SaldoEstoque saldo = saldos.travar(nova.canteiroId(), nova.itemId())
            .orElseThrow(() -> new IllegalStateException("Saldo não encontrado depois de criado."));
        BigDecimal saldoApos = nova.tipo().aplicar(saldo.getQuantidade(), quantidade);
        if (saldoApos.signum() < 0) {
            throw new BusinessException(ErrorCode.SALDO_INSUFICIENTE,
                "Saldo insuficiente: há " + Quantidades.normalizar(saldo.getQuantidade()).toPlainString()
                    + " no canteiro.")
                .com("campo", "quantidade")
                .com("saldoAtual", Quantidades.normalizar(saldo.getQuantidade()));
        }

        MovimentacaoEstoque movimentacao = movimentacoes.save(new MovimentacaoEstoque(nova.id(), nova.canteiroId(),
            nova.itemId(), nova.tipo(), nova.motivo(), quantidade, saldoApos, nova.colaboradorId(),
            nova.frenteObra(), nova.observacao(), nova.estornaId(), nova.registradoPor(),
            OffsetDateTime.now(relogio)));
        saldo.setQuantidade(saldoApos);
        return movimentacao;
    }

    /**
     * Confere que a quantidade é positiva e tem no máximo 3 casas decimais.
     *
     * @throws BusinessException {@code QUANTIDADE_INVALIDA}
     */
    static BigDecimal validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw quantidadeInvalida("A quantidade deve ser maior que zero.");
        }
        if (quantidade.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw quantidadeInvalida("A quantidade aceita no máximo 3 casas decimais.");
        }
        BigDecimal ajustada = quantidade.setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY);
        if (ajustada.precision() > 14) {
            throw quantidadeInvalida("A quantidade é grande demais.");
        }
        return ajustada;
    }

    private static BusinessException quantidadeInvalida(String mensagem) {
        return new BusinessException(ErrorCode.QUANTIDADE_INVALIDA, mensagem).com("campo", "quantidade");
    }
}
