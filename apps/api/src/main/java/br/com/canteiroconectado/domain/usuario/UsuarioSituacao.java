package br.com.canteiroconectado.domain.usuario;

import java.util.UUID;

public record UsuarioSituacao(
    boolean ativo,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean precisaTrocarSenha
) {
}
