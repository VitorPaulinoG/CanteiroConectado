package br.com.canteiroconectado.requisicao;

import java.util.Map;

/**
 * Contadores de requisições do Início.
 *
 * @param paraDecidir pendentes do outro perfil no canteiro, que o usuário pode decidir
 * @param minhas requisições do usuário no canteiro por status, com zero nos status sem nenhuma
 */
public record ContadoresDeRequisicoes(long paraDecidir, Map<StatusRequisicao, Long> minhas) {
}
