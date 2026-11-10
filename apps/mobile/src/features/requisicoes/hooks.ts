import { useQueryClient } from '@tanstack/react-query';

import { useEnvio } from '@/shared/api';
import { chavesEstoque } from '@/shared/estoque';

import {
  chavesRequisicoes,
  criarRequisicao,
  type PedidoDeRequisicao,
  type Requisicao,
} from './api';

export function useAtualizarRequisicoes(canteiroId: string) {
  const queryClient = useQueryClient();
  return async (requisicao?: Requisicao) => {
    if (requisicao) {
      queryClient.setQueryData(chavesRequisicoes.detalhe(requisicao.id), requisicao);
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: chavesRequisicoes.todas }),
      queryClient.invalidateQueries({ queryKey: chavesEstoque.resumo(canteiroId) }),
    ]);
  };
}

export function useCriarRequisicao(canteiroId: string) {
  const atualizar = useAtualizarRequisicoes(canteiroId);
  return useEnvio<PedidoDeRequisicao, Requisicao>({
    mutationKey: ['requisicoes', 'criar', canteiroId],
    mutationFn: (pedido) => criarRequisicao(pedido),
    aoSalvar: (requisicao) => atualizar(requisicao),
  });
}
