package br.com.canteiroconectado.requisicao;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.canteiroconectado.usuario.Perfil;

/**
 * Requisição completa (S14), com itens, linha do tempo e as ações permitidas ao usuário logado.
 *
 * @param numero número legível, como REQ-0042
 */
public record RequisicaoResponse(
    UUID id,
    String numero,
    UUID canteiroId,
    String canteiroNome,
    UUID solicitanteId,
    String solicitanteNome,
    Perfil solicitantePerfil,
    String frenteObra,
    String observacao,
    LocalDate necessarioAte,
    StatusRequisicao status,
    UUID decididoPorId,
    String decididoPorNome,
    OffsetDateTime decididoEm,
    String observacaoDecisao,
    LocalDate previsaoChegada,
    OffsetDateTime criadoEm,
    List<ItemDaRequisicaoResponse> itens,
    List<EventoDaRequisicaoResponse> historico,
    AcoesDaRequisicao acoes
) {

    RequisicaoResponse comAcoes(AcoesDaRequisicao novasAcoes) {
        return new RequisicaoResponse(id, numero, canteiroId, canteiroNome, solicitanteId, solicitanteNome,
            solicitantePerfil, frenteObra, observacao, necessarioAte, status, decididoPorId, decididoPorNome,
            decididoEm, observacaoDecisao, previsaoChegada, criadoEm, itens, historico, novasAcoes);
    }
}
