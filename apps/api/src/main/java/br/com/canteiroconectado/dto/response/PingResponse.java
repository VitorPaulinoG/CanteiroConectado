package br.com.canteiroconectado.dto.response;

import java.time.Instant;

public record PingResponse(String nome, String versao, Instant horaServidor) {
}
