package br.com.canteiroconectado.dto.request;

import java.util.UUID;

import br.com.canteiroconectado.domain.usuario.Perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar em branco")
    String nome,

    @Email(message = "Informe um e-mail válido")
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres")
    String email,

    Perfil perfil,

    UUID canteiroId
) {
}
