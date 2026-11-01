package br.com.canteiroconectado.auditoria;

import java.util.Set;

/**
 * Ações sensíveis registradas na trilha de auditoria ({@code log_auditoria.acao}).
 */
public enum AcaoAuditoria {
    ADMIN_CRIADO,
    LOGIN_SUCESSO,
    LOGIN_FALHOU,
    CONTA_BLOQUEADA,
    SENHA_TROCADA,
    CANTEIRO_CRIADO,
    CANTEIRO_EDITADO,
    USUARIO_CRIADO,
    USUARIO_EDITADO,
    USUARIO_DESATIVADO,
    USUARIO_ATIVADO,
    SENHA_REDEFINIDA,
    CREDENCIAL_CONSULTADA,
    COLABORADOR_CRIADO,
    COLABORADOR_EDITADO,
    ITEM_CRIADO,
    ITEM_EDITADO,
    REQUISICAO_CRIADA,
    REQUISICAO_APROVADA,
    REQUISICAO_RECUSADA,
    REQUISICAO_ATENDIDA,
    REQUISICAO_CANCELADA,
    MOVIMENTACAO_ESTORNADA;

    /**
     * Eventos que a história E11-H3 exige na trilha: login (sucesso, falha, bloqueio), criação do
     * administrador, criação, desativação e redefinição de funcionário, consulta de credencial
     * inicial, troca de senha, criação e edição de canteiro e item, estorno e decisão de
     * requisição. O teste {@code EventosObrigatoriosTest} falha se algum deixar de ser gravado.
     */
    public static final Set<AcaoAuditoria> OBRIGATORIAS = Set.of(
        LOGIN_SUCESSO, LOGIN_FALHOU, CONTA_BLOQUEADA, ADMIN_CRIADO, USUARIO_CRIADO, USUARIO_DESATIVADO,
        SENHA_REDEFINIDA, CREDENCIAL_CONSULTADA, SENHA_TROCADA, CANTEIRO_CRIADO, CANTEIRO_EDITADO, ITEM_CRIADO,
        ITEM_EDITADO, MOVIMENTACAO_ESTORNADA, REQUISICAO_APROVADA, REQUISICAO_RECUSADA);
}
