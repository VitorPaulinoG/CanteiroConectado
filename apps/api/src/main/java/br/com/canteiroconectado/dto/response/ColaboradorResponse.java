package br.com.canteiroconectado.dto.response;

import java.util.UUID;

import br.com.canteiroconectado.dominio.colaborador.Colaborador;

public record ColaboradorResponse(UUID id, String nome, UUID canteiroId, boolean ativo) {

    public static ColaboradorResponse of(Colaborador colaborador) {
        return new ColaboradorResponse(colaborador.getId(), colaborador.getNome(), colaborador.getCanteiroId(),
            colaborador.isAtivo());
    }
}
