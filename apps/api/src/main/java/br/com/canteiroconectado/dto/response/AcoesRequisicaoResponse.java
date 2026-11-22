package br.com.canteiroconectado.dto.response;

import java.util.UUID;

import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;
import br.com.canteiroconectado.domain.usuario.Perfil;
import br.com.canteiroconectado.security.UsuarioLogado;

public record AcoesRequisicaoResponse(boolean decidir, boolean atender, boolean cancelar) {

    public static AcoesRequisicaoResponse para(UsuarioLogado usuario, UUID solicitanteId, Perfil solicitantePerfil,
            StatusRequisicao status) {
        boolean autor = usuario.id().equals(solicitanteId);
        return new AcoesRequisicaoResponse(
            status == StatusRequisicao.PENDENTE && !autor && usuario.perfil() != solicitantePerfil,
            status == StatusRequisicao.APROVADA && usuario.isAlmoxarife(),
            status == StatusRequisicao.PENDENTE && autor);
    }
}
