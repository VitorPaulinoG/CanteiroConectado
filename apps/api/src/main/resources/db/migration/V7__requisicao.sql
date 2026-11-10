CREATE TABLE requisicao (
    id                  UUID PRIMARY KEY,
    numero              INTEGER     NOT NULL,
    canteiro_id         UUID        NOT NULL REFERENCES canteiro (id),
    solicitante_id      UUID        NOT NULL REFERENCES usuario (id),
    solicitante_perfil  TEXT        NOT NULL CHECK (solicitante_perfil IN ('ALMOXARIFE', 'GESTOR')),
    frente_obra         TEXT,
    observacao          TEXT,
    necessario_ate      DATE        NOT NULL,
    status              TEXT        NOT NULL DEFAULT 'PENDENTE'
                        CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'ATENDIDA', 'CANCELADA')),
    decidido_por        UUID        REFERENCES usuario (id),
    decidido_em         TIMESTAMPTZ,
    observacao_decisao  TEXT,
    previsao_chegada    DATE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao              BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT requisicao_numero_por_canteiro UNIQUE (canteiro_id, numero)
);

CREATE INDEX requisicao_canteiro_status_data ON requisicao (canteiro_id, status, criado_em DESC);
CREATE INDEX requisicao_solicitante_data ON requisicao (solicitante_id, criado_em DESC);

CREATE TABLE item_requisicao (
    id             UUID PRIMARY KEY,
    requisicao_id  UUID           NOT NULL REFERENCES requisicao (id),
    item_id        UUID           NOT NULL REFERENCES item (id),
    quantidade     NUMERIC(14, 3) NOT NULL CHECK (quantidade > 0),
    ordem          INTEGER        NOT NULL,

    CONSTRAINT item_requisicao_item_unico UNIQUE (requisicao_id, item_id)
);

CREATE TABLE requisicao_sequencia (
    canteiro_id  UUID    PRIMARY KEY REFERENCES canteiro (id),
    ultimo       INTEGER NOT NULL
);

CREATE TABLE requisicao_evento (
    id             UUID PRIMARY KEY,
    requisicao_id  UUID        NOT NULL REFERENCES requisicao (id),
    status         TEXT        NOT NULL
                   CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'ATENDIDA', 'CANCELADA')),
    usuario_id     UUID        NOT NULL REFERENCES usuario (id),
    observacao     TEXT,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX requisicao_evento_requisicao ON requisicao_evento (requisicao_id, criado_em);

CREATE TRIGGER impedir_alteracao_requisicao_evento
    BEFORE UPDATE OR DELETE ON requisicao_evento
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao();
