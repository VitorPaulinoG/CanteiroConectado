import { useQueryClient } from '@tanstack/react-query';

import { useEnvio } from '@/shared/api';
import { chavesEstoque, type Movimentacao } from '@/shared/estoque';

import {
  estornarMovimentacao,
  registrarMovimentacao,
  type PedidoDeEstorno,
  type PedidoDeMovimentacao,
} from './api';

export const CHAVE_DAS_MOVIMENTACOES = ['movimentacoes'] as const;

export function useRegistrarMovimentacao(canteiroId: string) {
  const queryClient = useQueryClient();
  return useEnvio<PedidoDeMovimentacao, Movimentacao>({
    mutationKey: ['movimentacoes', 'registrar', canteiroId],
    mutationFn: (pedido) => registrarMovimentacao(canteiroId, pedido),
    aoSalvar: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: chavesEstoque.doCanteiro(canteiroId) }),
        queryClient.invalidateQueries({ queryKey: CHAVE_DAS_MOVIMENTACOES }),
      ]);
    },
  });
}

export function useEstornar(movimentacao: Movimentacao) {
  const queryClient = useQueryClient();
  return useEnvio<PedidoDeEstorno, Movimentacao>({
    mutationKey: ['movimentacoes', 'estornar', movimentacao.id],
    mutationFn: (pedido) => estornarMovimentacao(movimentacao.id, pedido),
    aoSalvar: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: chavesEstoque.doCanteiro(movimentacao.canteiroId),
        }),
        queryClient.invalidateQueries({ queryKey: CHAVE_DAS_MOVIMENTACOES }),
      ]);
    },
  });
}
