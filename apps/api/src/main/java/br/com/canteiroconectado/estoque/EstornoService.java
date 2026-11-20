package br.com.canteiroconectado.estoque;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.comum.IdDoCliente;
import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.comum.ResultadoDaGravacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;

/**
 * Detalhe de uma movimentação e o estorno, que corrige um lançamento errado sem apagá-lo: grava
 * uma movimentação contrária de motivo {@code ESTORNO}, com a mesma quantidade, apontando para a
 * original. Cada movimentação só pode ser estornada uma vez, e um estorno não pode ser estornado.
 */
@Service
public class EstornoService {

    static final String ENTIDADE = "movimentacao";

    private final MovimentacaoEstoqueRepository movimentacoes;
    private final MovimentacaoConsultaRepository consultas;
    private final MovimentacaoService aplicacao;
    private final CanteiroAcessoService acesso;
    private final AuditoriaService auditoria;

    public EstornoService(MovimentacaoEstoqueRepository movimentacoes, MovimentacaoConsultaRepository consultas,
            MovimentacaoService aplicacao, CanteiroAcessoService acesso, AuditoriaService auditoria) {
        this.movimentacoes = movimentacoes;
        this.consultas = consultas;
        this.aplicacao = aplicacao;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    /**
     * Registro completo de uma movimentação, com os vínculos de estorno. Os dois perfis leem; o
     * ALMOXARIFE só do próprio canteiro.
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public MovimentacaoResponse buscar(UUID id) {
        MovimentacaoResponse movimentacao = consultas.buscar(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Movimentação não encontrada."));
        acesso.conferir(movimentacao.canteiroId());
        return movimentacao;
    }

    /**
     * Estorna a movimentação, ou devolve o estorno já gravado com o mesmo id.
     *
     * @throws BusinessException {@code ID_INVALIDO}, {@code MOTIVO_DO_ESTORNO_OBRIGATORIO},
     *     {@code ESTORNO_NAO_ESTORNAVEL}, {@code JA_ESTORNADA}, {@code ACESSO_NEGADO},
     *     {@code SALDO_INSUFICIENTE} (estorno de entrada já consumida) ou {@code ID_JA_UTILIZADO}
     */
    @PreAuthorize(Permissoes.ALMOXARIFE)
    public ResultadoDaGravacao<MovimentacaoResponse> estornar(UUID originalId, EstornoRequest pedido) {
        UUID id = IdDoCliente.ler(pedido.id());
        String motivo = RegistroDeMovimentacaoService.texto(pedido.motivo());
        if (motivo == null) {
            throw new BusinessException(ErrorCode.MOTIVO_DO_ESTORNO_OBRIGATORIO,
                "Conte por que o lançamento está errado.").com("campo", "motivo");
        }
        MovimentacaoEstoque original = movimentacoes.findById(originalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Movimentação não encontrada."));
        acesso.conferir(original.getCanteiroId());
        if (original.getMotivo() == MotivoMovimentacao.ESTORNO) {
            throw new BusinessException(ErrorCode.ESTORNO_NAO_ESTORNAVEL,
                "Um estorno não pode ser estornado. Se ele foi um engano, registre a movimentação de novo.");
        }

        UsuarioLogado usuario = UsuarioLogado.atual();
        NovaMovimentacao estorno = new NovaMovimentacao(id, original.getCanteiroId(), original.getItemId(),
            original.getTipo().contrario(), MotivoMovimentacao.ESTORNO, original.getQuantidade(),
            original.getColaboradorId(), original.getFrenteObra(), motivo, original.getId(), usuario.id());

        Optional<ResultadoDaGravacao<MovimentacaoEstoque>> jaGravado = aplicacao.jaGravada(estorno);
        if (jaGravado.isPresent()) {
            return resposta(jaGravado.get());
        }
        if (movimentacoes.existsByEstornaId(original.getId())) {
            throw jaEstornada();
        }
        try {
            return resposta(aplicacao.aplicarUmaVez(estorno, criado -> auditar(usuario, original, criado, motivo)));
        } catch (DataIntegrityViolationException corrida) {
            if (movimentacoes.existsByEstornaId(original.getId())) {
                throw jaEstornada();
            }
            throw corrida;
        }
    }

    private void auditar(UsuarioLogado usuario, MovimentacaoEstoque original, MovimentacaoEstoque estorno,
            String motivo) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("estornoId", estorno.getId().toString());
        detalhes.put("canteiroId", original.getCanteiroId().toString());
        detalhes.put("itemId", original.getItemId().toString());
        detalhes.put("tipoOriginal", original.getTipo().name());
        detalhes.put("quantidade", Quantidades.normalizar(original.getQuantidade()).toPlainString());
        detalhes.put("motivo", motivo);
        auditoria.registrar(usuario.id(), AcaoAuditoria.MOVIMENTACAO_ESTORNADA, ENTIDADE, original.getId(), detalhes);
    }

    private ResultadoDaGravacao<MovimentacaoResponse> resposta(ResultadoDaGravacao<MovimentacaoEstoque> resultado) {
        MovimentacaoResponse estorno = consultas.buscar(resultado.registro().getId())
            .orElseThrow(() -> new IllegalStateException("Estorno gravado não encontrado."));
        return new ResultadoDaGravacao<>(estorno, resultado.novo());
    }

    private static BusinessException jaEstornada() {
        return new BusinessException(ErrorCode.JA_ESTORNADA, "Esta movimentação já foi estornada.");
    }
}
