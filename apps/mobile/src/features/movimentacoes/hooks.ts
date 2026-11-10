import { useMutation } from '@tanstack/react-query';

import { registrarMovimentacao, type PedidoDeMovimentacao } from './api';

export function useRegistrarMovimentacao(canteiroId: string) {
  return useMutation({
    mutationKey: ['movimentacoes', 'registrar', canteiroId],
    mutationFn: (pedido: PedidoDeMovimentacao) => registrarMovimentacao(canteiroId, pedido),
  });
}
