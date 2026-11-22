package br.com.canteiroconectado.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record AprovarRequisicaoRequest(
    LocalDate previsaoChegada,

    @Size(max = 200, message = "A observação deve ter no máximo 200 caracteres")
    String observacao
) {
}
