package br.com.canteiroconectado.domain.estoque;

import java.io.Serializable;
import java.util.UUID;

public record SaldoEstoqueId(UUID canteiroId, UUID itemId) implements Serializable {
}
