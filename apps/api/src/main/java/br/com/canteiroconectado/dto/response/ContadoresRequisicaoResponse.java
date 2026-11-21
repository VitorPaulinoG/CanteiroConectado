package br.com.canteiroconectado.dto.response;

import java.util.Map;

import br.com.canteiroconectado.dominio.requisicao.StatusRequisicao;

public record ContadoresRequisicaoResponse(long paraDecidir, Map<StatusRequisicao, Long> minhas) {
}
