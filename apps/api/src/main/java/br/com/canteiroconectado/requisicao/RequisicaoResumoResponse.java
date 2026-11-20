package br.com.canteiroconectado.requisicao;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.canteiroconectado.usuario.Perfil;

/**
 * Cartão da lista de requisições (S12): número, itens resumidos, prazo, previsão e status.
 */
public record RequisicaoResumoResponse(
    UUID id,
    String numero,
    UUID canteiroId,
    StatusRequisicao status,
    UUID solicitanteId,
    String solicitanteNome,
    Perfil solicitantePerfil,
    String frenteObra,
    LocalDate necessarioAte,
    LocalDate previsaoChegada,
    OffsetDateTime criadoEm,
    List<ItemDaRequisicaoResponse> itens
) {
}
