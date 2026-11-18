import type { Movimentacao } from '@/shared/estoque';
import { useEnvio } from '@/shared/api';

import { registrarMovimentacao, type PedidoDeMovimentacao } from './api';

export function useRegistrarMovimentacao(
  canteiroId: string,
  aoSalvar?: (movimentacao: Movimentacao) => void | Promise<void>,
) {
  return useEnvio<PedidoDeMovimentacao, Movimentacao>({
    mutationKey: ['movimentacoes', 'registrar', canteiroId],
    mutationFn: (pedido) => registrarMovimentacao(canteiroId, pedido),
    aoSalvar,
  });
}
