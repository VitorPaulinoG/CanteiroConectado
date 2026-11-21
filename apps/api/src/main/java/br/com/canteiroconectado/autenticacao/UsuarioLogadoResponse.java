package br.com.canteiroconectado.autenticacao;

import java.util.UUID;

import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;

/**
 * Dados do usuário autenticado, devolvidos no login e em {@code GET /autenticacao/eu}.
 *
 * @param canteiroId canteiro do ALMOXARIFE; nulo para GESTOR
 * @param precisaTrocarSenha verdadeiro enquanto a senha inicial não for trocada
 */
public record UsuarioLogadoResponse(
    UUID id,
    String nome,
    String email,
    Perfil perfil,
    boolean administrador,
    UUID canteiroId,
    boolean precisaTrocarSenha
) {

    static UsuarioLogadoResponse de(Usuario usuario) {
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
