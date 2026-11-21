package br.com.canteiroconectado.estoque;

import java.util.Set;

/**
 * Motivo da movimentação. Entrada: recebimento, devolução e ajuste. Saída: retirada e ajuste.
 * {@link #ESTORNO} só nasce pela correção de um lançamento, nunca pelo registro comum.
 */
public enum MotivoMovimentacao {
    RECEBIMENTO,
    DEVOLUCAO,
    RETIRADA,
    AJUSTE,
    ESTORNO;

    private static final Set<MotivoMovimentacao> DA_ENTRADA = Set.of(RECEBIMENTO, DEVOLUCAO, AJUSTE);
    private static final Set<MotivoMovimentacao> DA_SAIDA = Set.of(RETIRADA, AJUSTE);

    /**
     * Diz se o motivo pode ser usado no registro comum de uma movimentação deste tipo.
     */
    public boolean aceitoEm(TipoMovimentacao tipo) {
        return (tipo == TipoMovimentacao.ENTRADA ? DA_ENTRADA : DA_SAIDA).contains(this);
    }
}
