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
--   * Estoque (V6): movimentações de RECEBIMENTO/RETIRADA e saldos para alguns itens nos dois canteiros
--     (o MAT-002 do Canteiro XPTO fica abaixo do mínimo, para exercitar o alerta de estoque baixo).
--   * Requisições (V7): 3 no Canteiro XPTO (PENDENTE, APROVADA e ATENDIDA), com itens, eventos e sequência.
--     Movimentações e requisições usam IDs determinísticos e são inseridas só se ainda não existirem
--     (são imutáveis/históricas, então não são sobrescritas); saldos existentes também não são alterados.
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
        OR to_regclass('public.item') IS NULL
        OR to_regclass('public.saldo_estoque') IS NULL
        OR to_regclass('public.movimentacao_estoque') IS NULL
        OR to_regclass('public.requisicao') IS NULL
        OR to_regclass('public.item_requisicao') IS NULL
        OR to_regclass('public.requisicao_sequencia') IS NULL
        OR to_regclass('public.requisicao_evento') IS NULL THEN
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

-- -------------------------------------------------------------------------------------------------
-- Estoque: movimentações (V6). saldo_apos já vem calculado; o saldo final de cada item é o da
-- movimentação mais recente. IDs determinísticos (md5 da chave) para a idempotência.
-- -------------------------------------------------------------------------------------------------

CREATE TEMP TABLE seed_mov ON COMMIT DROP AS
SELECT m.chave, c.id AS canteiro_id, i.id AS item_id, m.tipo, m.motivo, m.quantidade, m.saldo_apos,
       col.id AS colaborador_id, m.frente_obra, m.observacao, m.registrado_por_email,
       now() - (m.dias_atras * interval '1 day') AS criado_em
  FROM (VALUES
        -- Canteiro XPTO (registrado pelo almoxarife Fulano)
        ('xpto-mat001-rec', 'canteiro xpto', 'MAT-001', 'ENTRADA', 'RECEBIMENTO', 100, 100, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat001-ret', 'canteiro xpto', 'MAT-001', 'SAIDA',   'RETIRADA',      30,  70, 'joão da silva', 'Fundação',  NULL,                      'fulano@canteiro.local', 10),
        ('xpto-mat002-rec', 'canteiro xpto', 'MAT-002', 'ENTRADA', 'RECEBIMENTO',   40,  40, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat002-ret', 'canteiro xpto', 'MAT-002', 'SAIDA',   'RETIRADA',      25,  15, 'maria souza',   'Alvenaria', NULL,                      'fulano@canteiro.local',  8),
        ('xpto-mat004-rec', 'canteiro xpto', 'MAT-004', 'ENTRADA', 'RECEBIMENTO',   20,  20, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat006-rec', 'canteiro xpto', 'MAT-006', 'ENTRADA', 'RECEBIMENTO', 3000,3000, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat006-ret', 'canteiro xpto', 'MAT-006', 'SAIDA',   'RETIRADA',    1200,1800, 'josé santos',   'Alvenaria', NULL,                      'fulano@canteiro.local',  5),
        ('xpto-mat008-rec', 'canteiro xpto', 'MAT-008', 'ENTRADA', 'RECEBIMENTO',  500, 500, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat011-rec', 'canteiro xpto', 'MAT-011', 'ENTRADA', 'RECEBIMENTO',   60,  60, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-mat015-rec', 'canteiro xpto', 'MAT-015', 'ENTRADA', 'RECEBIMENTO',   10,  10, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-fer001-rec', 'canteiro xpto', 'FER-001', 'ENTRADA', 'RECEBIMENTO',    8,   8, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-fer001-ret', 'canteiro xpto', 'FER-001', 'SAIDA',   'RETIRADA',       3,   5, 'pedro lima',    'Carpintaria', NULL,                    'fulano@canteiro.local',  3),
        ('xpto-fer001-dev', 'canteiro xpto', 'FER-001', 'ENTRADA', 'DEVOLUCAO',      1,   6, 'pedro lima',    'Carpintaria', NULL,                    'fulano@canteiro.local',  1),
        ('xpto-fer003-rec', 'canteiro xpto', 'FER-003', 'ENTRADA', 'RECEBIMENTO',    6,   6, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        ('xpto-fer011-rec', 'canteiro xpto', 'FER-011', 'ENTRADA', 'RECEBIMENTO',    2,   2, NULL,            NULL,        'Recebimento inicial',     'fulano@canteiro.local', 20),
        -- Canteiro ABC (registrado pelo gestor Beltrano)
        ('abc-mat001-rec',  'canteiro abc',  'MAT-001', 'ENTRADA', 'RECEBIMENTO',   50,  50, NULL,            NULL,        'Recebimento inicial',     'beltrano@canteiro.local', 15),
        ('abc-mat005-rec',  'canteiro abc',  'MAT-005', 'ENTRADA', 'RECEBIMENTO',   12,  12, NULL,            NULL,        'Recebimento inicial',     'beltrano@canteiro.local', 15),
        ('abc-fer003-rec',  'canteiro abc',  'FER-003', 'ENTRADA', 'RECEBIMENTO',    4,   4, NULL,            NULL,        'Recebimento inicial',     'beltrano@canteiro.local', 15)
       ) AS m (chave, canteiro_nome, item_codigo, tipo, motivo, quantidade, saldo_apos, colaborador_nome,
               frente_obra, observacao, registrado_por_email, dias_atras)
  JOIN canteiro c ON lower(c.nome) = m.canteiro_nome
  JOIN item i ON lower(i.codigo) = lower(m.item_codigo)
  LEFT JOIN colaborador col ON col.canteiro_id = c.id AND lower(col.nome) = m.colaborador_nome;

INSERT INTO movimentacao_estoque (id, canteiro_id, item_id, tipo, motivo, quantidade, saldo_apos,
                                  colaborador_id, frente_obra, observacao, registrado_por, criado_em)
SELECT md5('seed:mov:' || m.chave)::uuid, m.canteiro_id, m.item_id, m.tipo, m.motivo, m.quantidade,
       m.saldo_apos, m.colaborador_id, m.frente_obra, m.observacao, u.id, m.criado_em
  FROM seed_mov m
  JOIN usuario u ON lower(u.email) = m.registrado_por_email
ON CONFLICT (id) DO NOTHING;

-- Saldo = saldo_apos da movimentação mais recente de cada (canteiro, item). Não sobrescreve saldos existentes.
INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade)
SELECT DISTINCT ON (m.canteiro_id, m.item_id) m.canteiro_id, m.item_id, m.saldo_apos
  FROM seed_mov m
 ORDER BY m.canteiro_id, m.item_id, m.criado_em DESC
ON CONFLICT (canteiro_id, item_id) DO NOTHING;

-- -------------------------------------------------------------------------------------------------
-- Requisições (V7) do Canteiro XPTO: solicitadas pelo almoxarife Fulano, decididas pelo gestor Beltrano.
-- -------------------------------------------------------------------------------------------------

INSERT INTO requisicao (id, numero, canteiro_id, solicitante_id, solicitante_perfil, frente_obra, observacao,
                        necessario_ate, status, decidido_por, decidido_em, observacao_decisao,
                        previsao_chegada, criado_em)
SELECT md5('seed:req:' || r.numero)::uuid, r.numero, c.id, fulano.id, 'ALMOXARIFE', r.frente_obra, r.observacao,
       current_date + r.prazo_dias, r.status,
       CASE WHEN r.status <> 'PENDENTE' THEN beltrano.id END,
       CASE WHEN r.status <> 'PENDENTE' THEN now() - ((r.dias_atras - 1) * interval '1 day') END,
       r.observacao_decisao, r.previsao_chegada, now() - (r.dias_atras * interval '1 day')
  FROM (VALUES
        (1, 'Fundação',  'Reposição de cimento',      7, 'PENDENTE', NULL,                  NULL::date,                  3),
        (2, 'Alvenaria', 'Cal para o reboco externo', 5, 'APROVADA', 'Compra autorizada.',  current_date + 3,            6),
        (3, 'Alvenaria', 'Tubos para a rede de esgoto', -2, 'ATENDIDA', 'Atendida pelo almoxarifado central.', NULL::date, 12)
       ) AS r (numero, frente_obra, observacao, prazo_dias, status, observacao_decisao, previsao_chegada, dias_atras)
  JOIN canteiro c ON lower(c.nome) = 'canteiro xpto'
  JOIN usuario fulano   ON lower(fulano.email)   = 'fulano@canteiro.local'
  JOIN usuario beltrano ON lower(beltrano.email) = 'beltrano@canteiro.local'
ON CONFLICT DO NOTHING;

INSERT INTO item_requisicao (id, requisicao_id, item_id, quantidade, ordem)
SELECT md5('seed:itemreq:' || ir.numero || ':' || ir.codigo)::uuid, md5('seed:req:' || ir.numero)::uuid,
       i.id, ir.quantidade, ir.ordem
  FROM (VALUES (1, 'MAT-001', 50, 1),
               (1, 'MAT-004', 10, 2),
               (2, 'MAT-002', 40, 1),
               (3, 'MAT-011', 30, 1),
               (3, 'MAT-012', 50, 2)
       ) AS ir (numero, codigo, quantidade, ordem)
  JOIN item i ON lower(i.codigo) = lower(ir.codigo)
 WHERE EXISTS (SELECT 1 FROM requisicao WHERE id = md5('seed:req:' || ir.numero)::uuid)
ON CONFLICT DO NOTHING;

-- Histórico de status (imutável). Toda requisição nasce PENDENTE por Fulano; as decididas ganham evento do Beltrano.
INSERT INTO requisicao_evento (id, requisicao_id, status, usuario_id, observacao, criado_em)
SELECT md5('seed:evt:' || e.numero || ':' || e.status)::uuid, md5('seed:req:' || e.numero)::uuid, e.status, u.id,
       e.observacao, now() - (e.dias_atras * interval '1 day')
  FROM (VALUES (1, 'PENDENTE', 'fulano@canteiro.local',   NULL,                  3),
               (2, 'PENDENTE', 'fulano@canteiro.local',   NULL,                  6),
               (2, 'APROVADA', 'beltrano@canteiro.local', 'Compra autorizada.',  5),
               (3, 'PENDENTE', 'fulano@canteiro.local',   NULL,                  12),
               (3, 'APROVADA', 'beltrano@canteiro.local', NULL,                  11),
               (3, 'ATENDIDA', 'beltrano@canteiro.local', 'Atendida pelo almoxarifado central.', 9)
       ) AS e (numero, status, email, observacao, dias_atras)
  JOIN usuario u ON lower(u.email) = e.email
 WHERE EXISTS (SELECT 1 FROM requisicao WHERE id = md5('seed:req:' || e.numero)::uuid)
ON CONFLICT (id) DO NOTHING;

-- Sequência de numeração por canteiro (nunca diminui).
INSERT INTO requisicao_sequencia (canteiro_id, ultimo)
SELECT c.id, 3 FROM canteiro c WHERE lower(c.nome) = 'canteiro xpto'
ON CONFLICT (canteiro_id) DO UPDATE SET ultimo = GREATEST(requisicao_sequencia.ultimo, EXCLUDED.ultimo);

COMMIT;
