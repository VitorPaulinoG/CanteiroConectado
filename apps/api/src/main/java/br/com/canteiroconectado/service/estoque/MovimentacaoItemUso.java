package br.com.canteiroconectado.service.estoque;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.canteiroconectado.domain.item.ItemUso;
import br.com.canteiroconectado.repository.estoque.MovimentacaoEstoqueRepository;

@Component
public class MovimentacaoItemUso implements ItemUso {

    private final MovimentacaoEstoqueRepository movimentacoes;

    MovimentacaoItemUso(MovimentacaoEstoqueRepository movimentacoes) {
        this.movimentacoes = movimentacoes;
    }

    @Override
    public boolean jaMovimentado(UUID itemId) {
        return movimentacoes.existsByItemId(itemId);
    }
}
