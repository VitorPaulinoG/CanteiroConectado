package br.com.canteiroconectado.dto.request;

import jakarta.validation.constraints.Size;

public record EstornarMovimentacaoRequest(
    String id,

    @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres")
    String motivo
) {

    @Override
    public String toString() {
        return "EstornoRequest[id=" + id + "]";
    }
}
