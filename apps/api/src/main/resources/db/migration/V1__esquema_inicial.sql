-- V1__esquema_inicial.sql
-- Infraestrutura de imutabilidade: função reutilizável usada por qualquer tabela que não
-- pode sofrer UPDATE/DELETE. 
-- Tabelas de negócio (movimentacao_estoque, log_auditoria, etc.) nascem nas migrations das 
-- respectivas funcionalidades, anexando esta trigger, ex.:
--   CREATE TRIGGER impedir_alteracao_log_auditoria
--       BEFORE UPDATE OR DELETE ON log_auditoria
--       FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao();

CREATE FUNCTION bloquear_alteracao() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Registro imutável: % em % não é permitido', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;
