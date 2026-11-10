import { useMutation, useQueryClient } from '@tanstack/react-query';

import { paraAppError, useEnvio } from '@/shared/api';
import { chavesEstoque } from '@/shared/estoque';

import {
  aprovarRequisicao,
  atenderRequisicao,
  cancelarRequisicao,
  chavesRequisicoes,
  criarRequisicao,
  recusarRequisicao,
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

export type Decisao =
  | { tipo: 'aprovar'; previsaoChegada?: string; observacao?: string }
  | { tipo: 'recusar'; motivo: string };

const CODIGOS_DE_ESTADO = new Set(['REQUISICAO_NAO_PENDENTE', 'REQUISICAO_NAO_APROVADA']);

export function useAndamentoDaRequisicao<P>(
  requisicao: Requisicao,
  executar: (pedido: P) => Promise<Requisicao>,
) {
  const queryClient = useQueryClient();
  const atualizar = useAtualizarRequisicoes(requisicao.canteiroId);
  return useMutation({
    mutationKey: ['requisicoes', 'andamento', requisicao.id],
    mutationFn: executar,
    onSuccess: (atualizada) => atualizar(atualizada),
    onError: async (erro) => {
      if (CODIGOS_DE_ESTADO.has(paraAppError(erro).codigo)) {
        await queryClient.invalidateQueries({ queryKey: chavesRequisicoes.detalhe(requisicao.id) });
      }
    },
  });
}

export function useDecidirRequisicao(requisicao: Requisicao) {
  return useAndamentoDaRequisicao<Decisao>(requisicao, (decisao) =>
    decisao.tipo === 'aprovar'
      ? aprovarRequisicao(requisicao.id, {
          previsaoChegada: decisao.previsaoChegada,
          observacao: decisao.observacao,
        })
      : recusarRequisicao(requisicao.id, decisao.motivo),
  );
}

export type Andamento = 'atender' | 'cancelar';

export function useAtenderOuCancelar(requisicao: Requisicao) {
  return useAndamentoDaRequisicao<Andamento>(requisicao, (acao) =>
    acao === 'atender' ? atenderRequisicao(requisicao.id) : cancelarRequisicao(requisicao.id),
  );
}
