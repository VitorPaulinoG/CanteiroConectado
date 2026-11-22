package br.com.canteiroconectado.service.estoque;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.domain.estoque.MovimentacaoEstoque;
import br.com.canteiroconectado.domain.estoque.RegistrarMovimentacaoCommand;
import br.com.canteiroconectado.domain.estoque.SaldoEstoque;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.estoque.MovimentacaoEstoqueRepository;
import br.com.canteiroconectado.repository.estoque.SaldoEstoqueRepository;
import br.com.canteiroconectado.service.canteiro.CanteiroService;
import br.com.canteiroconectado.service.item.ItemService;
import br.com.canteiroconectado.util.Quantidades;
import br.com.canteiroconectado.util.ResultadoGravacao;

@Service
public class MovimentacaoService {

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

    public ResultadoGravacao<MovimentacaoEstoque> aplicarUmaVez(RegistrarMovimentacaoCommand nova) {
        return aplicarUmaVez(nova, criada -> { });
    }

    public ResultadoGravacao<MovimentacaoEstoque> aplicarUmaVez(RegistrarMovimentacaoCommand nova,
            Consumer<MovimentacaoEstoque> aoCriar) {
        Optional<ResultadoGravacao<MovimentacaoEstoque>> gravada = jaGravada(nova);
        if (gravada.isPresent()) {
            return gravada.get();
        }
        try {
            return escrita.execute(status -> {
                SaldoEstoque saldo = validarETravar(nova);
                Optional<ResultadoGravacao<MovimentacaoEstoque>> gravadaEnquantoEsperava = repetida(nova);
                if (gravadaEnquantoEsperava.isPresent()) {
                    return gravadaEnquantoEsperava.get();
                }
                MovimentacaoEstoque criada = gravar(nova, saldo);
                aoCriar.accept(criada);
                return ResultadoGravacao.criado(criada);
            });
        } catch (DataIntegrityViolationException corrida) {
            return leitura.execute(status -> repetida(nova)).orElseThrow(() -> corrida);
        }
    }

    public Optional<ResultadoGravacao<MovimentacaoEstoque>> jaGravada(RegistrarMovimentacaoCommand nova) {
        return leitura.execute(status -> repetida(nova));
    }

    @Transactional
    public MovimentacaoEstoque aplicar(RegistrarMovimentacaoCommand nova) {
        return gravar(nova, validarETravar(nova));
    }

    private SaldoEstoque validarETravar(RegistrarMovimentacaoCommand nova) {
        validarQuantidade(nova.quantidade());
        canteiros.exigirAtivo(nova.canteiroId());
        itens.exigirAtivo(nova.itemId());
        saldos.criarSeAusente(nova.canteiroId(), nova.itemId());
        return saldos.travar(nova.canteiroId(), nova.itemId())
            .orElseThrow(() -> new IllegalStateException("Saldo não encontrado depois de criado."));
    }

    private MovimentacaoEstoque gravar(RegistrarMovimentacaoCommand nova, SaldoEstoque saldo) {
        if (nova.estornaId() != null && movimentacoes.existsByEstornaId(nova.estornaId())) {
            throw new BusinessException(ErrorCode.JA_ESTORNADA, "Esta movimentação já foi estornada.");
        }
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

    private Optional<ResultadoGravacao<MovimentacaoEstoque>> repetida(RegistrarMovimentacaoCommand nova) {
        return movimentacoes.findById(nova.id()).map(existente -> {
            if (!mesmoConteudo(existente, nova)) {
                throw new BusinessException(ErrorCode.ID_JA_UTILIZADO,
                    "Este id já foi usado em outra movimentação. Gere um id novo para um registro novo.")
                    .com("campo", "id");
            }
            return ResultadoGravacao.repetido(existente);
        });
    }

    static boolean mesmoConteudo(MovimentacaoEstoque existente, RegistrarMovimentacaoCommand nova) {
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

    static BigDecimal validarQuantidade(BigDecimal quantidade) {
        return Quantidades.validar(quantidade, "quantidade");
    }
}
