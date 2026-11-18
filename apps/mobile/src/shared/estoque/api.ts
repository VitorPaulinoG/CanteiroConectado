import { api, type Pagina } from '@/shared/api';
import type { TipoItem, Unidade } from '@/shared/item';

export type StatusEstoque = 'ZERADO' | 'ABAIXO_DO_MINIMO' | 'NORMAL';

export type ItemEstoque = {
  itemId: string;
  codigo: string;
  descricao: string;
  unidade: Unidade;
  unidadeSimbolo: string;
  tipo: TipoItem;
  quantidadeMinima: number;
  saldo: number;
  status: StatusEstoque;
};

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA';
export type MotivoMovimentacao = 'RECEBIMENTO' | 'DEVOLUCAO' | 'RETIRADA' | 'AJUSTE' | 'ESTORNO';

export type Movimentacao = {
  id: string;
  canteiroId: string;
  canteiroNome: string;
  itemId: string;
  itemCodigo: string;
  itemDescricao: string;
  unidadeSimbolo: string;
  tipo: TipoMovimentacao;
  motivo: MotivoMovimentacao;
  quantidade: number;
  saldoApos: number;
  colaboradorId: string | null;
  colaboradorNome: string | null;
  frenteObra: string | null;
  observacao: string | null;
  estornaId: string | null;
  estornadaPorId: string | null;
  registradoPorId: string;
  registradoPorNome: string;
  criadoEm: string;
};

export type DetalheEstoque = Omit<ItemEstoque, 'itemId'> & {
  itemId: string;
  ativo: boolean;
  ultimasMovimentacoes: Movimentacao[];
};

export type FiltroEstoque = { q?: string; status?: StatusEstoque[]; tipo?: TipoItem };

export const chavesEstoque = {
  todos: ['estoque'] as const,
  doCanteiro: (canteiroId: string) => ['estoque', canteiroId] as const,
  lista: (canteiroId: string, filtro: FiltroEstoque) =>
    ['estoque', canteiroId, 'lista', filtro] as const,
  detalhe: (canteiroId: string, itemId: string) =>
    ['estoque', canteiroId, 'detalhe', itemId] as const,
  resumo: (canteiroId: string) => ['estoque', canteiroId, 'resumo'] as const,
};

export async function listarEstoque(
  canteiroId: string,
  filtro: FiltroEstoque,
  pagina: number,
): Promise<Pagina<ItemEstoque>> {
  const resposta = await api.get<Pagina<ItemEstoque>>(`/api/v1/canteiros/${canteiroId}/estoque`, {
    params: { ...filtro, pagina },
    paramsSerializer: { indexes: null },
  });
  return resposta.data;
}

export async function buscarDetalheEstoque(
  canteiroId: string,
  itemId: string,
): Promise<DetalheEstoque> {
  const resposta = await api.get<DetalheEstoque>(
    `/api/v1/canteiros/${canteiroId}/estoque/${itemId}`,
  );
  return resposta.data;
}
