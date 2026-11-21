package br.com.canteiroconectado.service.estoque;

import org.springframework.stereotype.Component;

import br.com.canteiroconectado.dominio.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.dominio.estoque.RegistrarMovimentacaoCommand;
import br.com.canteiroconectado.dominio.estoque.TipoMovimentacao;
import br.com.canteiroconectado.dominio.item.TipoItem;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.service.colaborador.ColaboradorService;
import br.com.canteiroconectado.service.item.ItemService;

@Component
public class MovimentacaoRegras {

    private final ColaboradorService colaboradores;
    private final ItemService itens;

    public MovimentacaoRegras(ColaboradorService colaboradores, ItemService itens) {
        this.colaboradores = colaboradores;
        this.itens = itens;
    }

    public void conferir(RegistrarMovimentacaoCommand nova) {
        if (nova.tipo() == TipoMovimentacao.SAIDA) {
            conferirSaida(nova);
        } else {
            conferirEntrada(nova);
        }
        if (nova.colaboradorId() != null) {
            colaboradores.exigirAtivoNoCanteiro(nova.colaboradorId(), nova.canteiroId());
        }
    }

    private static void conferirSaida(RegistrarMovimentacaoCommand nova) {
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

    private void conferirEntrada(RegistrarMovimentacaoCommand nova) {
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
