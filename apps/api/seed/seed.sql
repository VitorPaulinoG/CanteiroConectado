-- =================================================================================================
-- seed.sql — dados de exemplo para DESENVOLVIMENTO LOCAL. Nunca rodar em produção.
-- =================================================================================================
--
-- O que este seed cria (todas as senhas: "password123"):
--   * Administrador: se já existe um (o criado pelo AdminInitializer a partir de ADMIN_EMAIL), a senha
--     dele é resetada; se não existe nenhum, cria "admin@canteiro.local".
--   * Canteiros: "Canteiro XPTO" e "Canteiro ABC".
--   * Usuários:  Fulano   (ALMOXARIFE do Canteiro XPTO) — fulano@canteiro.local
--                Beltrano (GESTOR, sem canteiro)        — beltrano@canteiro.local
--   * Colaboradores: 5 por canteiro.
--   * Itens: 40 no catálogo (20 FERRAMENTA com código FER-xxx e 20 MATERIAL com código MAT-xxx).
--     Nenhum saldo nem movimentação é criado: o estoque começa zerado.
--
-- Todos os usuários tocados aqui ficam ativos, desbloqueados e sem troca de senha obrigatória.
--
-- PRÉ-REQUISITO: as migrations do Flyway já precisam ter rodado, ou seja, a API precisa ter subido
-- pelo menos uma vez contra este banco (via `docker compose up` ou `./mvnw spring-boot:run`). Se as
-- tabelas não existirem, o seed para com erro sem gravar nada.
--
-- É idempotente: pode rodar quantas vezes quiser. Registros já existentes (mesmo nome/e-mail/código,
-- sem diferenciar maiúsculas) são atualizados para os valores daqui em vez de duplicados. Tudo roda
-- numa única transação: ou entra tudo, ou nada.
--
-- COMO USAR (a partir da raiz do repositório):
--
--   1) Pelo Docker Compose (recomendado). O serviço "seed" do docker-compose.yml fica no profile
--      "seed", então NÃO sobe junto com `docker compose up`; só roda quando chamado:
--
--        docker compose up -d db              # e a API, pelo compose ou localmente, ao menos 1 vez
--        docker compose --profile seed run --rm seed
--
--   2) Pelo container do banco já em execução, sem o serviço "seed":
--
--        docker compose exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
--          < apps/api/seed/seed.sql
--
--   3) Com um psql instalado no host (porta 5432 publicada pelo compose):
--
--        psql -v ON_ERROR_STOP=1 -h localhost -U canteiro -d canteiroconectado -f apps/api/seed/seed.sql
--
-- O arquivo usa só SQL puro (sem meta-comandos do psql), então também roda em clientes gráficos
-- como DBeaver ou pgAdmin.
--
-- A senha é gravada como hash BCrypt pré-calculado, compatível com o BCryptPasswordEncoder do
-- SecurityConfig (custo 10). Para trocar a senha do seed, gere um novo hash, por exemplo:
--   python3 -c "import bcrypt; print(bcrypt.hashpw(b'nova-senha1', bcrypt.gensalt(10, prefix=b'2a')).decode())"
-- =================================================================================================

BEGIN;

-- Falha cedo, com mensagem clara, se as migrations ainda não rodaram.
DO $$
BEGIN
    IF to_regclass('public.usuario') IS NULL
        OR to_regclass('public.canteiro') IS NULL
        OR to_regclass('public.colaborador') IS NULL
        OR to_regclass('public.item') IS NULL THEN
        RAISE EXCEPTION 'Tabelas não encontradas: suba a API ao menos uma vez para o Flyway aplicar as migrations antes do seed.';
    END IF;
END;
$$;

-- Hash BCrypt de "password123".
CREATE TEMP TABLE seed_parametro ON COMMIT DROP AS
SELECT '$2a$10$U/z3TY8kYrMuy4FjN4rSs.2Wv6uIgbfeb7alfG7819EmvBV.PC/RG'::text AS senha_hash;

-- -------------------------------------------------------------------------------------------------
-- Administrador
-- -------------------------------------------------------------------------------------------------

-- Reseta a senha de quem já é administrador.
UPDATE usuario
   SET senha_hash               = (SELECT senha_hash FROM seed_parametro),
       senha_temporaria_cifrada = NULL,
       precisa_trocar_senha     = FALSE,
       tentativas_falhas        = 0,
       bloqueado_ate            = NULL,
       ativo                    = TRUE
 WHERE administrador;

-- Cria um administrador só se ainda não houver nenhum.
INSERT INTO usuario (nome, email, senha_hash, perfil, administrador, canteiro_id)
SELECT 'Administrador Local', 'admin@canteiro.local', p.senha_hash, 'GESTOR', TRUE, NULL
  FROM seed_parametro p
 WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE administrador)
ON CONFLICT ((lower(email))) DO UPDATE
   SET perfil                   = 'GESTOR',
       administrador            = TRUE,
       canteiro_id              = NULL,
       senha_hash               = EXCLUDED.senha_hash,
       senha_temporaria_cifrada = NULL,
       precisa_trocar_senha     = FALSE,
       tentativas_falhas        = 0,
       bloqueado_ate            = NULL,
       ativo                    = TRUE;

-- -------------------------------------------------------------------------------------------------
-- Canteiros
-- -------------------------------------------------------------------------------------------------

INSERT INTO canteiro (nome, endereco)
VALUES ('Canteiro XPTO', 'Rua das Obras, 100 - Centro'),
       ('Canteiro ABC',  'Avenida dos Construtores, 2500 - Distrito Industrial')
ON CONFLICT ((lower(nome))) DO UPDATE
   SET endereco = EXCLUDED.endereco,
       ativo    = TRUE;

-- -------------------------------------------------------------------------------------------------
-- Usuários (ALMOXARIFE exige canteiro; GESTOR não tem canteiro — CHECK da migration V2)
-- -------------------------------------------------------------------------------------------------

INSERT INTO usuario (nome, email, senha_hash, perfil, administrador, canteiro_id)
SELECT u.nome, u.email, p.senha_hash, u.perfil, FALSE, c.id
  FROM (VALUES ('Fulano',   'fulano@canteiro.local',   'ALMOXARIFE', 'canteiro xpto'),
               ('Beltrano', 'beltrano@canteiro.local', 'GESTOR',     NULL)
       ) AS u (nome, email, perfil, canteiro_nome)
  LEFT JOIN canteiro c ON lower(c.nome) = u.canteiro_nome
 CROSS JOIN seed_parametro p
ON CONFLICT ((lower(email))) DO UPDATE
   SET nome                     = EXCLUDED.nome,
       perfil                   = EXCLUDED.perfil,
       administrador            = FALSE,
       canteiro_id              = EXCLUDED.canteiro_id,
       senha_hash               = EXCLUDED.senha_hash,
       senha_temporaria_cifrada = NULL,
       precisa_trocar_senha     = FALSE,
       tentativas_falhas        = 0,
       bloqueado_ate            = NULL,
       ativo                    = TRUE;

-- -------------------------------------------------------------------------------------------------
-- Colaboradores (5 por canteiro)
-- -------------------------------------------------------------------------------------------------

INSERT INTO colaborador (nome, canteiro_id)
SELECT col.nome, c.id
  FROM (VALUES ('canteiro xpto', 'João da Silva'),
               ('canteiro xpto', 'Maria Souza'),
               ('canteiro xpto', 'José Santos'),
               ('canteiro xpto', 'Ana Oliveira'),
               ('canteiro xpto', 'Pedro Lima'),
               ('canteiro abc',  'Carlos Pereira'),
               ('canteiro abc',  'Francisca Costa'),
               ('canteiro abc',  'Antônio Ferreira'),
               ('canteiro abc',  'Luiza Almeida'),
               ('canteiro abc',  'Paulo Rodrigues')
       ) AS col (canteiro_nome, nome)
  JOIN canteiro c ON lower(c.nome) = col.canteiro_nome
ON CONFLICT (canteiro_id, (lower(nome))) DO UPDATE
   SET ativo = TRUE;

-- -------------------------------------------------------------------------------------------------
-- Itens do catálogo (20 ferramentas + 20 materiais)
-- -------------------------------------------------------------------------------------------------

INSERT INTO item (codigo, descricao, unidade, tipo, quantidade_minima)
VALUES
    -- Ferramentas
    ('FER-001', 'Martelo de unha 27 mm',                   'UN',    'FERRAMENTA', 4),
    ('FER-002', 'Marreta 1 kg',                            'UN',    'FERRAMENTA', 2),
    ('FER-003', 'Trena 5 m',                               'UN',    'FERRAMENTA', 4),
    ('FER-004', 'Nível de bolha 60 cm',                    'UN',    'FERRAMENTA', 2),
    ('FER-005', 'Colher de pedreiro 8"',                   'UN',    'FERRAMENTA', 6),
    ('FER-006', 'Desempenadeira de aço lisa',              'UN',    'FERRAMENTA', 4),
    ('FER-007', 'Pá quadrada',                             'UN',    'FERRAMENTA', 4),
    ('FER-008', 'Enxada larga',                            'UN',    'FERRAMENTA', 2),
    ('FER-009', 'Picareta com cabo',                       'UN',    'FERRAMENTA', 2),
    ('FER-010', 'Carrinho de mão 60 L',                    'UN',    'FERRAMENTA', 2),
    ('FER-011', 'Furadeira de impacto 650 W',              'UN',    'FERRAMENTA', 1),
    ('FER-012', 'Serra circular 7 1/4"',                   'UN',    'FERRAMENTA', 1),
    ('FER-013', 'Esmerilhadeira angular 4 1/2"',           'UN',    'FERRAMENTA', 1),
    ('FER-014', 'Betoneira 400 L',                         'UN',    'FERRAMENTA', 1),
    ('FER-015', 'Escada de alumínio 7 degraus',            'UN',    'FERRAMENTA', 1),
    ('FER-016', 'Alicate universal 8"',                    'UN',    'FERRAMENTA', 2),
    ('FER-017', 'Jogo de chaves de fenda e Phillips',      'UN',    'FERRAMENTA', 1),
    ('FER-018', 'Serrote 22"',                             'UN',    'FERRAMENTA', 2),
    ('FER-019', 'Talhadeira 10"',                          'UN',    'FERRAMENTA', 2),
    ('FER-020', 'Prumo de face 500 g',                     'UN',    'FERRAMENTA', 2),
    -- Materiais
    ('MAT-001', 'Cimento CP II-E-32 50 kg',                'SACO',  'MATERIAL',   30),
    ('MAT-002', 'Cal hidratada CH-III 20 kg',              'SACO',  'MATERIAL',   20),
    ('MAT-003', 'Argamassa colante AC-II 20 kg',           'SACO',  'MATERIAL',   15),
    ('MAT-004', 'Areia média lavada',                      'M3',    'MATERIAL',   5),
    ('MAT-005', 'Brita 1',                                 'M3',    'MATERIAL',   5),
    ('MAT-006', 'Tijolo cerâmico 8 furos 9x19x19 cm',      'UN',    'MATERIAL',   1000),
    ('MAT-007', 'Bloco de concreto 14x19x39 cm',           'UN',    'MATERIAL',   500),
    ('MAT-008', 'Vergalhão CA-50 10 mm',                   'KG',    'MATERIAL',   200),
    ('MAT-009', 'Arame recozido nº 18',                    'KG',    'MATERIAL',   10),
    ('MAT-010', 'Prego com cabeça 17x27',                  'KG',    'MATERIAL',   10),
    ('MAT-011', 'Tubo PVC esgoto 100 mm',                  'M',     'MATERIAL',   30),
    ('MAT-012', 'Tubo PVC soldável 25 mm',                 'M',     'MATERIAL',   50),
    ('MAT-013', 'Fio flexível 2,5 mm²',                    'M',     'MATERIAL',   100),
    ('MAT-014', 'Piso cerâmico 60x60 cm',                  'M2',    'MATERIAL',   40),
    ('MAT-015', 'Tinta acrílica branca fosca 18 L',        'LATA',  'MATERIAL',   4),
    ('MAT-016', 'Massa acrílica 18 L',                     'LATA',  'MATERIAL',   3),
    ('MAT-017', 'Impermeabilizante para laje',             'LITRO', 'MATERIAL',   20),
    ('MAT-018', 'Aditivo plastificante para concreto',     'LITRO', 'MATERIAL',   10),
    ('MAT-019', 'Parafuso com bucha S8 (100 un.)',         'CAIXA', 'MATERIAL',   2),
    ('MAT-020', 'Disco de corte diamantado 110 mm (10 un.)', 'CAIXA', 'MATERIAL', 1)
ON CONFLICT ((lower(codigo))) DO UPDATE
   SET descricao         = EXCLUDED.descricao,
       unidade           = EXCLUDED.unidade,
       tipo              = EXCLUDED.tipo,
       quantidade_minima = EXCLUDED.quantidade_minima,
       ativo             = TRUE;

COMMIT;
