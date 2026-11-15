package br.com.canteiroconectado.estoque;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import br.com.canteiroconectado.comum.IdDoCliente;
import br.com.canteiroconectado.comum.ResultadoDaGravacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

/**
 * Registro de entradas e saídas pelo app: só o ALMOXARIFE, no próprio canteiro. Confere as regras
 * do pedido e entrega a aplicação ao {@link MovimentacaoService}, que garante saldo coerente e
 * gravação única por id.
 */
@Service
public class RegistroDeMovimentacaoService {

    private final MovimentacaoService movimentacoes;
    private final MovimentacaoConsultaRepository consultas;
    private final CanteiroAcessoService acesso;
    private final RegrasDaMovimentacao regras;

    public RegistroDeMovimentacaoService(MovimentacaoService movimentacoes, MovimentacaoConsultaRepository consultas,
            CanteiroAcessoService acesso, RegrasDaMovimentacao regras) {
        this.movimentacoes = movimentacoes;
        this.consultas = consultas;
        this.acesso = acesso;
        this.regras = regras;
    }

    /**
     * Registra a movimentação ou devolve a já gravada com o mesmo id.
     *
     * @throws BusinessException {@code ID_INVALIDO}, {@code MOTIVO_INVALIDO}, {@code ACESSO_NEGADO},
     *     {@code ID_JA_UTILIZADO}, as recusas de {@link RegrasDaMovimentacao} e as do saldo
     */
    @PreAuthorize(Permissoes.ALMOXARIFE)
    public ResultadoDaGravacao<MovimentacaoResponse> registrar(UUID canteiroId, MovimentacaoRequest requisicao) {
        UUID id = IdDoCliente.ler(requisicao.id());
        acesso.conferir(canteiroId);
        if (!requisicao.motivo().aceitoEm(requisicao.tipo())) {
            throw new BusinessException(ErrorCode.MOTIVO_INVALIDO, motivoInvalido(requisicao.tipo()))
                .com("campo", "motivo");
        }

        NovaMovimentacao nova = new NovaMovimentacao(id, canteiroId, requisicao.itemId(), requisicao.tipo(),
            requisicao.motivo(), requisicao.quantidade(), requisicao.colaboradorId(), texto(requisicao.frenteObra()),
            texto(requisicao.observacao()), null, UsuarioLogado.atual().id());
        ResultadoDaGravacao<MovimentacaoEstoque> resultado = movimentacoes.jaGravada(nova).orElseGet(() -> {
            regras.conferir(nova);
            return movimentacoes.aplicarUmaVez(nova);
        });
        MovimentacaoResponse resposta = consultas.buscar(resultado.registro().getId())
            .orElseThrow(() -> new IllegalStateException("Movimentação gravada não encontrada."));
        return new ResultadoDaGravacao<>(resposta, resultado.novo());
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static String motivoInvalido(TipoMovimentacao tipo) {
        return tipo == TipoMovimentacao.ENTRADA
            ? "Na entrada, use o motivo Recebimento, Devolução ou Ajuste."
            : "Na saída, use o motivo Retirada ou Ajuste.";
    }
}
