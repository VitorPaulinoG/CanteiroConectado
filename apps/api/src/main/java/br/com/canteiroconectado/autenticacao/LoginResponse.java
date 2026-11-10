package br.com.canteiroconectado.autenticacao;

import java.time.Instant;

/**
 * Resposta do login.
 *
 * @param token JWT de acesso, enviado depois no cabeçalho {@code Authorization: Bearer}
 * @param expiraEm instante em que o token deixa de valer
 * @param usuario dados do usuário autenticado
 */
public record LoginResponse(
    String token,
    Instant expiraEm,
    UsuarioLogadoResponse usuario
) {
}
