package br.com.canteiroconectado.dto.response;

import java.util.List;
import java.util.Map;

import br.com.canteiroconectado.dominio.requisicao.StatusRequisicao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumoResponse(
    long zerados,
    @JsonProperty("abaixoDoMinimo") long abaixoMinimo,
    List<ItemEstoqueResponse> maisCriticos,
    long requisicoesParaDecidir,
    Map<StatusRequisicao, Long> minhasRequisicoes
) {
}
