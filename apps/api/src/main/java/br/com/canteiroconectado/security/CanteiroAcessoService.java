package br.com.canteiroconectado.security;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;

/**
 * Confere se o usuário logado pode acessar um canteiro: o ALMOXARIFE só o próprio, o GESTOR
 * todos. Os services chamam {@link #conferir(UUID)} em toda operação que recebe {@code canteiroId}.
 */
@Service("canteiroAcesso")
public class CanteiroAcessoService {

    /**
     * Diz se o usuário logado pode acessar o canteiro.
     */
    public boolean pode(UUID canteiroId) {
        return pode(UsuarioLogado.atual(), canteiroId);
    }

    /**
     * Diz se o usuário informado pode acessar o canteiro.
     */
    public boolean pode(UsuarioLogado usuario, UUID canteiroId) {
        if (canteiroId == null) {
            return false;
        }
        return usuario.isGestor() || canteiroId.equals(usuario.canteiroId());
    }

    /**
     * Recusa com {@code ACESSO_NEGADO} quando o usuário logado não pode acessar o canteiro.
     */
    public void conferir(UUID canteiroId) {
        if (!pode(canteiroId)) {
            throw new BusinessException(ErrorCode.ACESSO_NEGADO, "Você não tem acesso a este canteiro.");
        }
    }

    /**
     * Devolve o canteiro a usar numa consulta: o informado, depois de conferido, ou o do próprio
     * ALMOXARIFE quando nenhum é informado.
     *
     * @throws BusinessException {@code VALIDACAO_INVALIDA} se um GESTOR não informar o canteiro, ou
     *     {@code ACESSO_NEGADO}
     */
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
