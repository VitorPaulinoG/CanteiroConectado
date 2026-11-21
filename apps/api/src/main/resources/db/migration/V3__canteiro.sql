CREATE TABLE canteiro (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       TEXT        NOT NULL,
    endereco   TEXT,
    ativo      BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX canteiro_nome_unico ON canteiro (lower(nome));

ALTER TABLE usuario
    ADD CONSTRAINT usuario_canteiro_fk FOREIGN KEY (canteiro_id) REFERENCES canteiro (id);
