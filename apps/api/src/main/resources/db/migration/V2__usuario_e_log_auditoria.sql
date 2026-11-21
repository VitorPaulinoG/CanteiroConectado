CREATE TABLE usuario (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                      TEXT        NOT NULL,
    email                     TEXT        NOT NULL,
    senha_hash                TEXT        NOT NULL,
    perfil                    TEXT        NOT NULL CHECK (perfil IN ('ALMOXARIFE', 'GESTOR')),
    administrador             BOOLEAN     NOT NULL DEFAULT FALSE,
    canteiro_id               UUID,
    ativo                     BOOLEAN     NOT NULL DEFAULT TRUE,
    senha_temporaria_cifrada  TEXT,
    precisa_trocar_senha      BOOLEAN     NOT NULL DEFAULT FALSE,
    tentativas_falhas         INTEGER     NOT NULL DEFAULT 0,
    bloqueado_ate             TIMESTAMPTZ,
    criado_em                 TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT usuario_canteiro_por_perfil CHECK (
        (perfil = 'ALMOXARIFE' AND canteiro_id IS NOT NULL)
        OR (perfil = 'GESTOR' AND canteiro_id IS NULL)
    ),
    CONSTRAINT usuario_administrador_so_gestor CHECK (
        administrador = FALSE OR perfil = 'GESTOR'
    )
);

CREATE UNIQUE INDEX usuario_email_unico ON usuario (lower(email));

CREATE TABLE log_auditoria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        REFERENCES usuario (id),
    acao        TEXT        NOT NULL,
    entidade    TEXT        NOT NULL,
    entidade_id UUID,
    detalhes    JSONB,
    request_id  TEXT,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX log_auditoria_criado_em ON log_auditoria (criado_em DESC);

CREATE TRIGGER impedir_alteracao_log_auditoria
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao();
