package br.com.canteiroconectado.requisicao;

import java.util.UUID;

import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.usuario.Perfil;

/**
 * O que o usuário logado pode fazer com a requisição agora, para o app mostrar só os botões
 * válidos. A API revalida cada ação.
 *
 * @param decidir aprovar ou recusar: requisição pendente de outro perfil
 * @param atender marcar como atendida: requisição aprovada e usuário almoxarife
 * @param cancelar requisição pendente criada pelo próprio usuário
 */
public record AcoesDaRequisicao(boolean decidir, boolean atender, boolean cancelar) {

    public static AcoesDaRequisicao para(UsuarioLogado usuario, UUID solicitanteId, Perfil solicitantePerfil,
            StatusRequisicao status) {
        boolean autor = usuario.id().equals(solicitanteId);
        return new AcoesDaRequisicao(
            status == StatusRequisicao.PENDENTE && !autor && usuario.perfil() != solicitantePerfil,
            status == StatusRequisicao.APROVADA && usuario.isAlmoxarife(),
            status == StatusRequisicao.PENDENTE && autor);
    }
}
