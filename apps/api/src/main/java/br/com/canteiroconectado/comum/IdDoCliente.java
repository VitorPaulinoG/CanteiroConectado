package br.com.canteiroconectado.comum;

import java.util.UUID;
import java.util.regex.Pattern;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

/**
 * Lê o {@code id} que o app gera para movimentações e requisições. O id é a chave primária do
 * registro: repetir o envio com o mesmo id nunca duplica o registro.
 */
public final class IdDoCliente {

    private static final Pattern UUID_CANONICO =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private IdDoCliente() {
    }

    /**
     * @throws BusinessException {@code ID_INVALIDO} quando o texto está vazio ou não é um UUID no
     *     formato {@code 8-4-4-4-12}
     */
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
