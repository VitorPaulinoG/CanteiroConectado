CREATE TABLE colaborador (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome         TEXT        NOT NULL,
    canteiro_id  UUID        NOT NULL REFERENCES canteiro (id),
    ativo        BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX colaborador_nome_unico_por_canteiro ON colaborador (canteiro_id, lower(nome));
