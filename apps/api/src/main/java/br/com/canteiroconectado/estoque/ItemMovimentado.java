package br.com.canteiroconectado.estoque;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.canteiroconectado.item.UsoDoItem;

/**
 * Informa ao catálogo se um item já está no livro de movimentações, para travar a mudança de
 * código e unidade.
 */
@Component
class ItemMovimentado implements UsoDoItem {

    private final MovimentacaoEstoqueRepository movimentacoes;

    ItemMovimentado(MovimentacaoEstoqueRepository movimentacoes) {
        this.movimentacoes = movimentacoes;
    }

    @Override
    public boolean jaMovimentado(UUID itemId) {
        return movimentacoes.existsByItemId(itemId);
    }
}
