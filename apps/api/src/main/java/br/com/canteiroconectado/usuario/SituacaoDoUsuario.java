package br.com.canteiroconectado.usuario;

import java.util.UUID;

/**
 * Situação do usuário conferida a cada requisição autenticada.
 */
public record SituacaoDoUsuario(
    boolean ativo,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean precisaTrocarSenha
) {
}
