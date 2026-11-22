package br.com.canteiroconectado.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;
import br.com.canteiroconectado.domain.usuario.Perfil;

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
    List<ItemRequisicaoResponse> itens,
    List<EventoRequisicaoResponse> historico,
    AcoesRequisicaoResponse acoes
) {

    public RequisicaoResponse comAcoes(AcoesRequisicaoResponse novasAcoes) {
        return new RequisicaoResponse(id, numero, canteiroId, canteiroNome, solicitanteId, solicitanteNome,
            solicitantePerfil, frenteObra, observacao, necessarioAte, status, decididoPorId, decididoPorNome,
            decididoEm, observacaoDecisao, previsaoChegada, criadoEm, itens, historico, novasAcoes);
    }
}
