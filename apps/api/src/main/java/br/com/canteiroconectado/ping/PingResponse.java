package br.com.canteiroconectado.ping;

import java.time.Instant;

/**
 * Resposta de {@code GET /api/v1/ping}.
 *
 * @param nome nome da aplicação
 * @param versao versão do artefato gerado pelo Maven
 * @param horaServidor hora atual do servidor, em UTC
 */
public record PingResponse(String nome, String versao, Instant horaServidor) {
}
