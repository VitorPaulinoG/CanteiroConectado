package br.com.canteiroconectado.service.estoque;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import br.com.canteiroconectado.dominio.estoque.MovimentacaoEstoque;
import br.com.canteiroconectado.dominio.estoque.RegistrarMovimentacaoCommand;
import br.com.canteiroconectado.dominio.estoque.TipoMovimentacao;
import br.com.canteiroconectado.dto.request.RegistrarMovimentacaoRequest;
import br.com.canteiroconectado.dto.response.MovimentacaoResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.estoque.MovimentacaoConsultaRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.service.canteiro.CanteiroAcessoService;
import br.com.canteiroconectado.util.IdCliente;
import br.com.canteiroconectado.util.ResultadoGravacao;

@Service
public class MovimentacaoRegistroService {

    private final MovimentacaoService movimentacoes;
    private final MovimentacaoConsultaRepository consultas;
    private final CanteiroAcessoService acesso;
    private final MovimentacaoRegras regras;

    public MovimentacaoRegistroService(MovimentacaoService movimentacoes, MovimentacaoConsultaRepository consultas,
            CanteiroAcessoService acesso, MovimentacaoRegras regras) {
        this.movimentacoes = movimentacoes;
        this.consultas = consultas;
        this.acesso = acesso;
        this.regras = regras;
    }

    @PreAuthorize(Permissoes.ALMOXARIFE)
    public ResultadoGravacao<MovimentacaoResponse> registrar(UUID canteiroId, RegistrarMovimentacaoRequest requisicao) {
        UUID id = IdCliente.ler(requisicao.id());
        acesso.conferir(canteiroId);
        if (!requisicao.motivo().aceitoEm(requisicao.tipo())) {
            throw new BusinessException(ErrorCode.MOTIVO_INVALIDO, motivoInvalido(requisicao.tipo()))
                .com("campo", "motivo");
        }

        RegistrarMovimentacaoCommand nova = new RegistrarMovimentacaoCommand(id, canteiroId, requisicao.itemId(), requisicao.tipo(),
            requisicao.motivo(), requisicao.quantidade(), requisicao.colaboradorId(), texto(requisicao.frenteObra()),
            texto(requisicao.observacao()), null, UsuarioLogado.atual().id());
        ResultadoGravacao<MovimentacaoEstoque> resultado = movimentacoes.jaGravada(nova).orElseGet(() -> {
            regras.conferir(nova);
            return movimentacoes.aplicarUmaVez(nova);
        });
        MovimentacaoResponse resposta = consultas.buscar(resultado.registro().getId())
            .orElseThrow(() -> new IllegalStateException("Movimentação gravada não encontrada."));
        return new ResultadoGravacao<>(resposta, resultado.novo());
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
