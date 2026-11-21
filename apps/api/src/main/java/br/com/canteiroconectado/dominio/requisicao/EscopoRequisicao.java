package br.com.canteiroconectado.dominio.requisicao;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

public enum EscopoRequisicao {
    MINHAS("minhas"),
    PARA_DECIDIR("para-decidir");

    private final String valor;

    EscopoRequisicao(String valor) {
        this.valor = valor;
    }

    public static EscopoRequisicao ler(String texto) {
        if (texto == null || texto.isBlank()) {
            return MINHAS;
        }
        for (EscopoRequisicao escopo : values()) {
            if (escopo.valor.equalsIgnoreCase(texto.strip())) {
                return escopo;
            }
        }
        throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "Use escopo=minhas ou escopo=para-decidir.")
            .com("campo", "escopo");
    }
}
