package br.com.canteiroconectado.requisicao;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

/**
 * Aba da lista de requisições (S12).
 *
 * <ul>
 *   <li>{@link #MINHAS}: as criadas pelo usuário logado no canteiro;</li>
 *   <li>{@link #PARA_DECIDIR}: as pendentes do outro perfil no canteiro, que o usuário pode
 *   aprovar ou recusar.</li>
 * </ul>
 */
public enum EscopoDeRequisicoes {
    MINHAS("minhas"),
    PARA_DECIDIR("para-decidir");

    private final String valor;

    EscopoDeRequisicoes(String valor) {
        this.valor = valor;
    }

    /**
     * @throws BusinessException {@code VALIDACAO_INVALIDA} para valor desconhecido
     */
    public static EscopoDeRequisicoes ler(String texto) {
        if (texto == null || texto.isBlank()) {
            return MINHAS;
        }
        for (EscopoDeRequisicoes escopo : values()) {
            if (escopo.valor.equalsIgnoreCase(texto.strip())) {
                return escopo;
            }
        }
        throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "Use escopo=minhas ou escopo=para-decidir.")
            .com("campo", "escopo");
    }
}
