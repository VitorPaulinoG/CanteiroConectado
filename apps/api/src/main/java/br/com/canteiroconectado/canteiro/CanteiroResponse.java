package br.com.canteiroconectado.canteiro;

import java.util.UUID;

/**
 * Canteiro devolvido pela API.
 */
public record CanteiroResponse(UUID id, String nome, String endereco, boolean ativo) {

    static CanteiroResponse de(Canteiro canteiro) {
        return new CanteiroResponse(canteiro.getId(), canteiro.getNome(), canteiro.getEndereco(),
            canteiro.isAtivo());
    }
}
