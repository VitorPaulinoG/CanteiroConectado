CREATE TABLE saldo_estoque (
    canteiro_id  UUID           NOT NULL REFERENCES canteiro (id),
    item_id      UUID           NOT NULL REFERENCES item (id),
    quantidade   NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    versao       BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (canteiro_id, item_id)
);

CREATE TABLE movimentacao_estoque (
    id              UUID PRIMARY KEY,
    canteiro_id     UUID           NOT NULL REFERENCES canteiro (id),
    item_id         UUID           NOT NULL REFERENCES item (id),
    tipo            TEXT           NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    motivo          TEXT           NOT NULL CHECK (motivo IN ('RECEBIMENTO', 'DEVOLUCAO', 'RETIRADA', 'AJUSTE', 'ESTORNO')),
    quantidade      NUMERIC(14, 3) NOT NULL CHECK (quantidade > 0),
    saldo_apos      NUMERIC(14, 3) NOT NULL CHECK (saldo_apos >= 0),
    colaborador_id  UUID           REFERENCES colaborador (id),
    frente_obra     TEXT,
    observacao      TEXT,
    estorna_id      UUID           UNIQUE REFERENCES movimentacao_estoque (id),
    registrado_por  UUID           NOT NULL REFERENCES usuario (id),
    criado_em       TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT movimentacao_estorno_tem_original CHECK ((motivo = 'ESTORNO') = (estorna_id IS NOT NULL))
);

CREATE INDEX movimentacao_canteiro_item_data ON movimentacao_estoque (canteiro_id, item_id, criado_em DESC);
CREATE INDEX movimentacao_canteiro_data ON movimentacao_estoque (canteiro_id, criado_em DESC);
CREATE INDEX movimentacao_colaborador ON movimentacao_estoque (colaborador_id) WHERE colaborador_id IS NOT NULL;

CREATE TRIGGER impedir_alteracao_movimentacao_estoque
    BEFORE UPDATE OR DELETE ON movimentacao_estoque
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao();
