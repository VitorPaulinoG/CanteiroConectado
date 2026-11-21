package br.com.canteiroconectado.dto.response;

import java.util.List;
import java.util.Map;

import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;

public record ResumoResponse(
    long zerados,
    long abaixoMinimo,
    List<ItemEstoqueResponse> maisCriticos,
    long requisicoesParaDecidir,
    Map<StatusRequisicao, Long> minhasRequisicoes
) {
}
