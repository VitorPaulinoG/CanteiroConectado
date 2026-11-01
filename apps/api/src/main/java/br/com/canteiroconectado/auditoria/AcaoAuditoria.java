package br.com.canteiroconectado.auditoria;

/**
 * Ações sensíveis registradas na trilha de auditoria ({@code log_auditoria.acao}).
 */
public enum AcaoAuditoria {
    ADMIN_CRIADO,
    LOGIN_SUCESSO,
    LOGIN_FALHOU,
    CONTA_BLOQUEADA
}
