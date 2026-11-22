package br.com.canteiroconectado.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarRequisicaoRequest(
    String id,

    UUID canteiroId,

    @NotNull(message = "Informe a data de necessidade")
    LocalDate necessarioAte,

    @Size(max = 60, message = "A frente de obra deve ter no máximo 60 caracteres")
    String frenteObra,

    @Size(max = 200, message = "A observação deve ter no máximo 200 caracteres")
    String observacao,

    @NotNull(message = "Informe os itens")
    List<@Valid @NotNull ItemRequisicaoRequest> itens
) {
}
