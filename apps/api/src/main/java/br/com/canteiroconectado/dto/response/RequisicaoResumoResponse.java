package br.com.canteiroconectado.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;
import br.com.canteiroconectado.domain.usuario.Perfil;

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
    List<ItemRequisicaoResponse> itens
) {
}
