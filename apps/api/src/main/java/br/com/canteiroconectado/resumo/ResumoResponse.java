package br.com.canteiroconectado.resumo;

import java.util.List;
import java.util.Map;

import br.com.canteiroconectado.estoque.ItemEstoqueResponse;
import br.com.canteiroconectado.requisicao.StatusRequisicao;

/**
 * Resumo do Início (S04) para o canteiro e o usuário logado.
 *
 * @param zerados itens ativos com mínimo maior que zero e saldo zero
 * @param abaixoDoMinimo itens ativos com saldo maior que zero e menor que o mínimo
 * @param maisCriticos os três itens em alerta mais críticos
 * @param requisicoesParaDecidir pendentes do outro perfil que o usuário pode decidir
 * @param minhasRequisicoes requisições do usuário por status
 */
public record ResumoResponse(
    long zerados,
    long abaixoDoMinimo,
    List<ItemEstoqueResponse> maisCriticos,
    long requisicoesParaDecidir,
    Map<StatusRequisicao, Long> minhasRequisicoes
) {
}
