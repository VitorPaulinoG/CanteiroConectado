package br.com.canteiroconectado.requisicao;

/**
 * Situação da requisição. Nasce {@link #PENDENTE}; a outra pessoa aprova ou recusa; o almoxarife
 * marca como atendida quando o material chega; o autor pode cancelar enquanto está pendente.
 */
public enum StatusRequisicao {
    PENDENTE,
    APROVADA,
    RECUSADA,
    ATENDIDA,
    CANCELADA
}
