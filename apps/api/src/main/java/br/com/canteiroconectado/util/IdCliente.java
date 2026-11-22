package br.com.canteiroconectado.util;

import java.util.UUID;
import java.util.regex.Pattern;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

public final class IdCliente {

    private static final Pattern UUID_CANONICO =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private IdCliente() {
    }

    public static UUID ler(String texto) {
        if (texto == null || texto.isBlank()) {
            throw invalido("Informe o id gerado no app.");
        }
        if (!UUID_CANONICO.matcher(texto.strip()).matches()) {
            throw invalido("O id deve ser um UUID válido.");
        }
        return UUID.fromString(texto.strip());
    }

    private static BusinessException invalido(String mensagem) {
        return new BusinessException(ErrorCode.ID_INVALIDO, mensagem).com("campo", "id");
    }
}
