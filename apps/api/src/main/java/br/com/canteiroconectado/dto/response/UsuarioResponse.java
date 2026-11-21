package br.com.canteiroconectado.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.canteiroconectado.domain.usuario.Perfil;
import br.com.canteiroconectado.domain.usuario.Usuario;

public record UsuarioResponse(
    UUID id,
    String nome,
    String email,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean ativo,
    boolean precisaTrocarSenha,
    boolean credencialInicialDisponivel,
    OffsetDateTime criadoEm
) {

    public static UsuarioResponse of(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
            usuario.getPerfil(), usuario.isAdministrador(), usuario.getCanteiroId(), usuario.isAtivo(),
            usuario.isPrecisaTrocarSenha(), usuario.getSenhaTemporariaCifrada() != null, usuario.getCriadoEm());
    }
}
