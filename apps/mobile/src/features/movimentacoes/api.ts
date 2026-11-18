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
