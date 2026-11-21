package br.com.canteiroconectado.dominio.item;

import java.util.UUID;

public interface ItemUso {

    boolean jaMovimentado(UUID itemId);
}
