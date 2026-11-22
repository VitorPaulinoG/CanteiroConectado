package br.com.canteiroconectado.domain.estoque;

import java.util.Set;

public enum MotivoMovimentacao {
    RECEBIMENTO,
    DEVOLUCAO,
    RETIRADA,
    AJUSTE,
    ESTORNO;

    private static final Set<MotivoMovimentacao> DA_ENTRADA = Set.of(RECEBIMENTO, DEVOLUCAO, AJUSTE);
    private static final Set<MotivoMovimentacao> DA_SAIDA = Set.of(RETIRADA, AJUSTE);

    public boolean aceitoEm(TipoMovimentacao tipo) {
        return (tipo == TipoMovimentacao.ENTRADA ? DA_ENTRADA : DA_SAIDA).contains(this);
    }
}
