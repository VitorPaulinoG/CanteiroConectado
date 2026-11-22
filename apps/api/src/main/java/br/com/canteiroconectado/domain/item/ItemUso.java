package br.com.canteiroconectado.domain.item;

import java.util.UUID;

public interface ItemUso {

    boolean jaMovimentado(UUID itemId);
}
