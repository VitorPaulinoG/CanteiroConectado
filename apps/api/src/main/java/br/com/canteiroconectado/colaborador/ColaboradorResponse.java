package br.com.canteiroconectado.colaborador;

import java.util.UUID;

public record ColaboradorResponse(UUID id, String nome, UUID canteiroId, boolean ativo) {

    static ColaboradorResponse de(Colaborador colaborador) {
        return new ColaboradorResponse(colaborador.getId(), colaborador.getNome(), colaborador.getCanteiroId(),
            colaborador.isAtivo());
    }
}
