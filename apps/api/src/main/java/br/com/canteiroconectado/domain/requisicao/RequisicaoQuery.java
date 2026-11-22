package br.com.canteiroconectado.domain.requisicao;

import java.util.Collection;
import java.util.UUID;

import br.com.canteiroconectado.domain.usuario.Perfil;

public record RequisicaoQuery(
    UUID canteiroId,
    UUID usuarioId,
    Perfil perfilQueEuDecido,
    EscopoRequisicao escopo,
    Collection<StatusRequisicao> status
) {
}
