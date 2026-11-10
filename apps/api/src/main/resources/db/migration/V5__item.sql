CREATE TABLE item (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo             TEXT           NOT NULL,
    descricao          TEXT           NOT NULL,
    unidade            TEXT           NOT NULL CHECK (unidade IN ('UN', 'KG', 'M', 'M2', 'M3', 'SACO', 'LATA', 'CAIXA', 'LITRO')),
    tipo               TEXT           NOT NULL CHECK (tipo IN ('MATERIAL', 'FERRAMENTA')),
    quantidade_minima  NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (quantidade_minima >= 0),
    ativo              BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX item_codigo_unico ON item (lower(codigo));
CREATE INDEX item_descricao ON item (lower(descricao));
