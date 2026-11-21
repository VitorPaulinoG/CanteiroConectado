package br.com.canteiroconectado.dto.response;

import java.util.UUID;

import br.com.canteiroconectado.domain.usuario.Perfil;
import br.com.canteiroconectado.domain.usuario.Usuario;

public record UsuarioLogadoResponse(
    UUID id,
    String nome,
    String email,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean precisaTrocarSenha
) {

    public static UsuarioLogadoResponse of(Usuario usuario) {
        return new UsuarioLogadoResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil(),
            usuario.isAdministrador(),
            usuario.getCanteiroId(),
            usuario.isPrecisaTrocarSenha());
    }
}
