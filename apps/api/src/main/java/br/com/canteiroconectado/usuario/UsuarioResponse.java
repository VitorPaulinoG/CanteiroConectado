package br.com.canteiroconectado.usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Funcionário devolvido ao administrador.
 *
 * @param precisaTrocarSenha verdadeiro enquanto a senha temporária não for trocada
 */
public record UsuarioResponse(
    UUID id,
    String nome,
    String email,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean ativo,
    boolean precisaTrocarSenha,
    OffsetDateTime criadoEm
) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
            usuario.getPerfil(), usuario.isAdministrador(), usuario.getCanteiroId(), usuario.isAtivo(),
            usuario.isPrecisaTrocarSenha(), usuario.getCriadoEm());
    }
}
