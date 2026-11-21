package br.com.canteiroconectado.service.canteiro;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.security.UsuarioLogado;

@Service("canteiroAcesso")
public class CanteiroAcessoService {

    public boolean pode(UUID canteiroId) {
        return pode(UsuarioLogado.atual(), canteiroId);
    }

    public boolean pode(UsuarioLogado usuario, UUID canteiroId) {
        if (canteiroId == null) {
            return false;
        }
        return usuario.isGestor() || canteiroId.equals(usuario.canteiroId());
    }

    public void conferir(UUID canteiroId) {
        if (!pode(canteiroId)) {
            throw new BusinessException(ErrorCode.ACESSO_NEGADO, "Você não tem acesso a este canteiro.");
        }
    }

    public UUID resolver(UUID canteiroIdInformado) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        UUID canteiroId = canteiroIdInformado != null ? canteiroIdInformado : usuario.canteiroId();
        if (canteiroId == null) {
            throw new BusinessException(ErrorCode.VALIDACAO_INVALIDA, "Informe o canteiro (canteiroId).");
        }
        conferir(canteiroId);
        return canteiroId;
    }
}
