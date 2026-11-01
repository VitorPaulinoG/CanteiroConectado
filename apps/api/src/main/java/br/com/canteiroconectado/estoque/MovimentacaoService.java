package br.com.canteiroconectado.estoque;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.comum.ResultadoDaGravacao;
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
 *
 * <p>O id vem do app. {@link #aplicarUmaVez(NovaMovimentacao)} devolve o registro já gravado
 * quando o mesmo pedido chega de novo, recusa com {@code ID_JA_UTILIZADO} quando o id já pertence
 * a outro conteúdo e trata a corrida entre duas gravações simultâneas pela violação da chave
 * primária.</p>
 */
@Service
public class MovimentacaoService {

    static final int CASAS_DECIMAIS = 3;

    private final MovimentacaoEstoqueRepository movimentacoes;
    private final SaldoEstoqueRepository saldos;
    private final CanteiroService canteiros;
    private final ItemService itens;
    private final Clock relogio;
    private final TransactionTemplate escrita;
    private final TransactionTemplate leitura;

    public MovimentacaoService(MovimentacaoEstoqueRepository movimentacoes, SaldoEstoqueRepository saldos,
            CanteiroService canteiros, ItemService itens, Clock relogio, PlatformTransactionManager transacoes) {
        this.movimentacoes = movimentacoes;
        this.saldos = saldos;
        this.canteiros = canteiros;
        this.itens = itens;
        this.relogio = relogio;
        this.escrita = new TransactionTemplate(transacoes);
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
    }

    /**
     * Aplica a movimentação uma única vez por id. Cada tentativa roda numa transação própria,
     * então este método não pode ser chamado dentro de outra transação.
     *
     * @return o registro e se ele foi criado agora ({@code novo}) ou já existia ({@code repetido})
     * @throws BusinessException {@code ID_JA_UTILIZADO} e as mesmas recusas de
     *     {@link #aplicar(NovaMovimentacao)}
     */
    public ResultadoDaGravacao<MovimentacaoEstoque> aplicarUmaVez(NovaMovimentacao nova) {
        Optional<ResultadoDaGravacao<MovimentacaoEstoque>> gravada = jaGravada(nova);
        if (gravada.isPresent()) {
            return gravada.get();
        }
        try {
            return escrita.execute(status -> {
                SaldoEstoque saldo = validarETravar(nova);
                Optional<ResultadoDaGravacao<MovimentacaoEstoque>> gravadaEnquantoEsperava = repetida(nova);
                if (gravadaEnquantoEsperava.isPresent()) {
                    return gravadaEnquantoEsperava.get();
                }
                return ResultadoDaGravacao.criado(gravar(nova, saldo));
            });
        } catch (DataIntegrityViolationException corrida) {
            return leitura.execute(status -> repetida(nova)).orElseThrow(() -> corrida);
        }
    }

    /**
     * Valida, bloqueia o saldo, grava a movimentação com {@code saldo_apos} e atualiza o saldo.
     *
     * @throws BusinessException {@code QUANTIDADE_INVALIDA}, {@code CANTEIRO_INATIVO},
     *     {@code ITEM_INATIVO} ou {@code SALDO_INSUFICIENTE} (com {@code saldoAtual})
     */
    /**
     * Procura o mesmo pedido já gravado, para quem precisa responder a um reenvio antes de
     * conferir regras que podem ter mudado (colaborador desativado, por exemplo).
     *
     * @throws BusinessException {@code ID_JA_UTILIZADO} quando o id tem outro conteúdo
     */
    public Optional<ResultadoDaGravacao<MovimentacaoEstoque>> jaGravada(NovaMovimentacao nova) {
        return leitura.execute(status -> repetida(nova));
    }

    @Transactional
    public MovimentacaoEstoque aplicar(NovaMovimentacao nova) {
        return gravar(nova, validarETravar(nova));
    }

    private SaldoEstoque validarETravar(NovaMovimentacao nova) {
        validarQuantidade(nova.quantidade());
        canteiros.exigirAtivo(nova.canteiroId());
        itens.exigirAtivo(nova.itemId());
        saldos.criarSeAusente(nova.canteiroId(), nova.itemId());
        return saldos.travar(nova.canteiroId(), nova.itemId())
            .orElseThrow(() -> new IllegalStateException("Saldo não encontrado depois de criado."));
    }

    private MovimentacaoEstoque gravar(NovaMovimentacao nova, SaldoEstoque saldo) {
        BigDecimal quantidade = validarQuantidade(nova.quantidade());
        BigDecimal saldoApos = nova.tipo().aplicar(saldo.getQuantidade(), quantidade);
        if (saldoApos.signum() < 0) {
            throw new BusinessException(ErrorCode.SALDO_INSUFICIENTE,
                "Saldo insuficiente: há " + Quantidades.normalizar(saldo.getQuantidade()).toPlainString()
                    + " no canteiro.")
                .com("campo", "quantidade")
                .com("saldoAtual", Quantidades.normalizar(saldo.getQuantidade()));
        }

        MovimentacaoEstoque movimentacao = movimentacoes.saveAndFlush(new MovimentacaoEstoque(nova.id(),
            nova.canteiroId(), nova.itemId(), nova.tipo(), nova.motivo(), quantidade, saldoApos,
            nova.colaboradorId(), nova.frenteObra(), nova.observacao(), nova.estornaId(), nova.registradoPor(),
            OffsetDateTime.now(relogio)));
        saldo.setQuantidade(saldoApos);
        return movimentacao;
    }

    private Optional<ResultadoDaGravacao<MovimentacaoEstoque>> repetida(NovaMovimentacao nova) {
        return movimentacoes.findById(nova.id()).map(existente -> {
            if (!mesmoConteudo(existente, nova)) {
                throw new BusinessException(ErrorCode.ID_JA_UTILIZADO,
                    "Este id já foi usado em outra movimentação. Gere um id novo para um registro novo.")
                    .com("campo", "id");
            }
            return ResultadoDaGravacao.repetido(existente);
        });
    }

    /**
     * Diz se a movimentação gravada é o mesmo pedido: mesmos canteiro, item, tipo, motivo,
     * quantidade, colaborador, frente, observação, vínculo de estorno e autor.
     */
    static boolean mesmoConteudo(MovimentacaoEstoque existente, NovaMovimentacao nova) {
        return existente.getCanteiroId().equals(nova.canteiroId())
            && existente.getItemId().equals(nova.itemId())
            && existente.getTipo() == nova.tipo()
            && existente.getMotivo() == nova.motivo()
            && nova.quantidade() != null && existente.getQuantidade().compareTo(nova.quantidade()) == 0
            && Objects.equals(existente.getColaboradorId(), nova.colaboradorId())
            && Objects.equals(existente.getFrenteObra(), nova.frenteObra())
            && Objects.equals(existente.getObservacao(), nova.observacao())
            && Objects.equals(existente.getEstornaId(), nova.estornaId())
            && existente.getRegistradoPor().equals(nova.registradoPor());
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
