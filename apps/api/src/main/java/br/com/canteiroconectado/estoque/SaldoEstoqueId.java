package br.com.canteiroconectado.estoque;

import java.io.Serializable;
import java.util.UUID;

/**
 * Chave composta do saldo: canteiro e item.
 */
public record SaldoEstoqueId(UUID canteiroId, UUID itemId) implements Serializable {
}
