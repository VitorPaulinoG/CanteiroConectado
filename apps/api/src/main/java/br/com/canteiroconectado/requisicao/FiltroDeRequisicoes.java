package br.com.canteiroconectado.requisicao;

import java.util.Collection;
import java.util.UUID;

import br.com.canteiroconectado.usuario.Perfil;

/**
 * Filtro já resolvido da lista: o canteiro, o usuário e o escopo, com o perfil cujas requisições o
 * usuário decide.
 *
 * @param status filtro opcional de status; vazio traz todos
 */
record FiltroDeRequisicoes(
    UUID canteiroId,
    UUID usuarioId,
    Perfil perfilQueEuDecido,
    EscopoDeRequisicoes escopo,
    Collection<StatusRequisicao> status
) {
}
