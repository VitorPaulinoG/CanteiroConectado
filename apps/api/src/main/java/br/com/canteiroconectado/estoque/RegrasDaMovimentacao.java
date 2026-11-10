package br.com.canteiroconectado.estoque;

import org.springframework.stereotype.Component;

import br.com.canteiroconectado.colaborador.ColaboradorService;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.item.ItemService;
import br.com.canteiroconectado.item.TipoItem;

/**
 * Regras de cada combinação de tipo e motivo do registro pelo app.
 *
 * <p>Saída: na retirada, colaborador e frente de obra são obrigatórios; no ajuste (perda, furto,
 * dano), o colaborador é opcional e a observação é obrigatória.</p>
 *
 * <p>Entrada: no recebimento, colaborador e observação (nota fiscal, fornecedor) são opcionais;
 * na devolução, o colaborador é obrigatório e o item precisa ser ferramenta; no ajuste, a
 * observação é obrigatória. O MVP não confere se a devolução é maior que a retirada anterior.</p>
 *
 * <p>Em qualquer caso, o colaborador informado precisa estar ativo e ser do mesmo canteiro.</p>
 */
@Component
public class RegrasDaMovimentacao {

    private final ColaboradorService colaboradores;
    private final ItemService itens;

    public RegrasDaMovimentacao(ColaboradorService colaboradores, ItemService itens) {
        this.colaboradores = colaboradores;
        this.itens = itens;
    }

    /**
     * @throws BusinessException {@code COLABORADOR_OBRIGATORIO}, {@code FRENTE_OBRA_OBRIGATORIA},
     *     {@code OBSERVACAO_OBRIGATORIA}, {@code DEVOLUCAO_SO_FERRAMENTA}, {@code ITEM_INATIVO} ou
     *     {@code COLABORADOR_INVALIDO}
     */
    public void conferir(NovaMovimentacao nova) {
        if (nova.tipo() == TipoMovimentacao.SAIDA) {
            conferirSaida(nova);
        } else {
            conferirEntrada(nova);
        }
        if (nova.colaboradorId() != null) {
            colaboradores.exigirAtivoNoCanteiro(nova.colaboradorId(), nova.canteiroId());
        }
    }

    private static void conferirSaida(NovaMovimentacao nova) {
        if (nova.motivo() == MotivoMovimentacao.RETIRADA) {
            exigir(nova.colaboradorId() != null, ErrorCode.COLABORADOR_OBRIGATORIO,
                "Informe o colaborador que está retirando.", "colaboradorId");
            exigir(nova.frenteObra() != null, ErrorCode.FRENTE_OBRA_OBRIGATORIA,
                "Informe a frente de obra.", "frenteObra");
        }
        if (nova.motivo() == MotivoMovimentacao.AJUSTE) {
            exigir(nova.observacao() != null, ErrorCode.OBSERVACAO_OBRIGATORIA,
                "No ajuste, conte o que aconteceu (perda, furto, dano...).", "observacao");
        }
    }

    private void conferirEntrada(NovaMovimentacao nova) {
        if (nova.motivo() == MotivoMovimentacao.DEVOLUCAO) {
            exigir(nova.colaboradorId() != null, ErrorCode.COLABORADOR_OBRIGATORIO,
                "Informe o colaborador que está devolvendo.", "colaboradorId");
            exigir(itens.exigirAtivo(nova.itemId()).getTipo() == TipoItem.FERRAMENTA, ErrorCode.DEVOLUCAO_SO_FERRAMENTA,
                "Só ferramentas podem ser devolvidas. Para material, use Ajuste.", "itemId");
        }
        if (nova.motivo() == MotivoMovimentacao.AJUSTE) {
            exigir(nova.observacao() != null, ErrorCode.OBSERVACAO_OBRIGATORIA,
                "No ajuste, conte o que aconteceu (contagem, sobra...).", "observacao");
        }
    }

    static void exigir(boolean condicao, ErrorCode codigo, String mensagem, String campo) {
        if (!condicao) {
            throw new BusinessException(codigo, mensagem).com("campo", campo);
        }
    }
}
