import { api } from '@/shared/api';
import type { Movimentacao, MotivoMovimentacao, TipoMovimentacao } from '@/shared/estoque';

export type PedidoDeMovimentacao = {
  id: string;
  tipo: TipoMovimentacao;
  motivo: MotivoMovimentacao;
  itemId: string;
  quantidade: number;
  colaboradorId?: string;
  frenteObra?: string;
  observacao?: string;
};

export async function registrarMovimentacao(
  canteiroId: string,
  pedido: PedidoDeMovimentacao,
): Promise<Movimentacao> {
  const resposta = await api.post<Movimentacao>(
    `/api/v1/canteiros/${canteiroId}/movimentacoes`,
    pedido,
  );
  return resposta.data;
}

export const chavesMovimentacao = {
  detalhe: (id: string) => ['movimentacoes', 'detalhe', id] as const,
};

export async function buscarMovimentacao(id: string): Promise<Movimentacao> {
  return (await api.get<Movimentacao>(`/api/v1/movimentacoes/${id}`)).data;
}

export type PedidoDeEstorno = { id: string; motivo: string };

export async function estornarMovimentacao(
  movimentacaoId: string,
  pedido: PedidoDeEstorno,
): Promise<Movimentacao> {
  const resposta = await api.post<Movimentacao>(
    `/api/v1/movimentacoes/${movimentacaoId}/estorno`,
    pedido,
  );
  return resposta.data;
}
