import { api, type Pagina } from '@/shared/api';

export type AcaoAuditoria =
  | 'ADMIN_CRIADO'
  | 'LOGIN_SUCESSO'
  | 'LOGIN_FALHOU'
  | 'CONTA_BLOQUEADA'
  | 'SENHA_TROCADA'
  | 'CANTEIRO_CRIADO'
  | 'CANTEIRO_EDITADO'
  | 'USUARIO_CRIADO'
  | 'USUARIO_EDITADO'
  | 'USUARIO_DESATIVADO'
  | 'USUARIO_ATIVADO'
  | 'SENHA_REDEFINIDA'
  | 'CREDENCIAL_CONSULTADA'
  | 'COLABORADOR_CRIADO'
  | 'COLABORADOR_EDITADO'
  | 'ITEM_CRIADO'
  | 'ITEM_EDITADO'
  | 'REQUISICAO_CRIADA'
  | 'REQUISICAO_APROVADA'
  | 'REQUISICAO_RECUSADA'
  | 'REQUISICAO_ATENDIDA'
  | 'REQUISICAO_CANCELADA'
  | 'MOVIMENTACAO_ESTORNADA';

export type EventoDeAuditoria = {
  id: string;
  acao: AcaoAuditoria;
  usuarioId: string | null;
  usuarioNome: string | null;
  entidade: string;
  entidadeId: string | null;
  entidadeDescricao: string | null;
  detalhes: Record<string, unknown>;
  requestId: string | null;
  criadoEm: string;
};

export type FiltroDaAuditoria = { acao?: AcaoAuditoria; de?: string; ate?: string };

export const chavesAuditoria = {
  lista: (filtro: FiltroDaAuditoria) => ['auditoria', filtro] as const,
};

export async function listarAuditoria(
  filtro: FiltroDaAuditoria,
  pagina: number,
): Promise<Pagina<EventoDeAuditoria>> {
  const resposta = await api.get<Pagina<EventoDeAuditoria>>('/api/v1/auditoria', {
    params: { ...filtro, pagina },
  });
  return resposta.data;
}
