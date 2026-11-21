package br.com.canteiroconectado.dto.response;

import java.time.Instant;

public record LoginResponse(
    String token,
    Instant expiraEm,
    UsuarioLogadoResponse usuario
) {
}
