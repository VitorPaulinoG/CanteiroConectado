import { api } from '@/shared/api';
import type { ItemEstoque } from '@/shared/estoque';

export type StatusDaMinhaRequisicao =
  'PENDENTE' | 'APROVADA' | 'RECUSADA' | 'ATENDIDA' | 'CANCELADA';

export type Resumo = {
  zerados: number;
  abaixoDoMinimo: number;
  maisCriticos: ItemEstoque[];
  requisicoesParaDecidir: number;
  minhasRequisicoes: Record<StatusDaMinhaRequisicao, number>;
};

export async function buscarResumo(canteiroId: string): Promise<Resumo> {
  return (await api.get<Resumo>(`/api/v1/canteiros/${canteiroId}/estoque/resumo`)).data;
}
