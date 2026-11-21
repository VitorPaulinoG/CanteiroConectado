package br.com.canteiroconectado.dto.response;

import java.util.UUID;

import br.com.canteiroconectado.dominio.canteiro.Canteiro;

public record CanteiroResponse(UUID id, String nome, String endereco, boolean ativo) {

    public static CanteiroResponse of(Canteiro canteiro) {
        return new CanteiroResponse(canteiro.getId(), canteiro.getNome(), canteiro.getEndereco(),
            canteiro.isAtivo());
    }
}
