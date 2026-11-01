package br.com.canteiroconectado.usuario;

/**
 * Situação do usuário conferida a cada requisição autenticada.
 */
public record SituacaoDoUsuario(boolean ativo, boolean administrador, boolean precisaTrocarSenha) {
}
