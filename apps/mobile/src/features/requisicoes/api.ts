import { api, type Pagina } from '@/shared/api';
import type { Perfil } from '@/shared/sessao';

export type StatusRequisicao = 'PENDENTE' | 'APROVADA' | 'RECUSADA' | 'ATENDIDA' | 'CANCELADA';
export type Escopo = 'minhas' | 'para-decidir';

export type ItemDaRequisicao = {
  itemId: string;
  codigo: string;
  descricao: string;
  unidadeSimbolo: string;
  quantidade: number;
};

export type EventoDaRequisicao = {
  status: StatusRequisicao;
  usuarioId: string;
  usuarioNome: string;
  observacao: string | null;
  criadoEm: string;
};

export type ResumoDaRequisicao = {
  id: string;
  numero: string;
  canteiroId: string;
  status: StatusRequisicao;
  solicitanteId: string;
  solicitanteNome: string;
  solicitantePerfil: Perfil;
  frenteObra: string | null;
  necessarioAte: string;
  previsaoChegada: string | null;
  criadoEm: string;
  itens: ItemDaRequisicao[];
};

export type Requisicao = ResumoDaRequisicao & {
  canteiroNome: string;
  observacao: string | null;
  decididoPorId: string | null;
  decididoPorNome: string | null;
  decididoEm: string | null;
  observacaoDecisao: string | null;
  historico: EventoDaRequisicao[];
  acoes: { decidir: boolean; atender: boolean; cancelar: boolean };
};

export type PedidoDeRequisicao = {
  id: string;
  canteiroId: string;
  necessarioAte: string;
  frenteObra?: string;
  observacao?: string;
  itens: { itemId: string; quantidade: number }[];
};

export const chavesRequisicoes = {
  todas: ['requisicoes'] as const,
  lista: (canteiroId: string, escopo: Escopo) =>
    ['requisicoes', canteiroId, 'lista', escopo] as const,
  detalhe: (id: string) => ['requisicoes', 'detalhe', id] as const,
};

const BASE = '/api/v1/requisicoes';

export async function criarRequisicao(pedido: PedidoDeRequisicao): Promise<Requisicao> {
  return (await api.post<Requisicao>(BASE, pedido)).data;
}

export async function listarRequisicoes(
  canteiroId: string,
  escopo: Escopo,
  pagina: number,
): Promise<Pagina<ResumoDaRequisicao>> {
  const resposta = await api.get<Pagina<ResumoDaRequisicao>>(BASE, {
    params: { canteiroId, escopo, pagina },
  });
  return resposta.data;
}

export async function buscarRequisicao(id: string): Promise<Requisicao> {
  return (await api.get<Requisicao>(`${BASE}/${id}`)).data;
}
