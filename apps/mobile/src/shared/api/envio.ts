import { useMutation, type MutationKey } from '@tanstack/react-query';
import { useCallback, useRef } from 'react';

import { paraAppError, type AppError } from './errors';

export type EstadoDeEnvio = 'parado' | 'enviando' | 'salvo' | 'falhou';

type Opcoes<P, R> = {
  mutationKey: MutationKey;
  mutationFn: (pedido: P) => Promise<R>;
  aoSalvar?: (resposta: R, pedido: P) => void | Promise<void>;
};

export type Envio<P, R> = {
  estado: EstadoDeEnvio;
  enviando: boolean;
  salvo: boolean;
  falhou: boolean;
  erro: AppError | null;
  semResposta: boolean;
  resposta: R | undefined;
  enviar: (pedido: P) => void;
  tentarDeNovo: () => void;
  reiniciar: () => void;
};

export function useEnvio<P, R>({ mutationKey, mutationFn, aoSalvar }: Opcoes<P, R>): Envio<P, R> {
  const ultimoPedido = useRef<P | null>(null);
  const emAndamento = useRef(false);
  const mutacao = useMutation({
    mutationKey,
    mutationFn,
    onSuccess: aoSalvar,
    onSettled: () => {
      emAndamento.current = false;
    },
  });
  const { mutate, reset } = mutacao;

  const disparar = useCallback(
    (pedido: P) => {
      if (emAndamento.current) {
        return;
      }
      emAndamento.current = true;
      ultimoPedido.current = pedido;
      mutate(pedido);
    },
    [mutate],
  );

  const tentarDeNovo = useCallback(() => {
    if (ultimoPedido.current) {
      disparar(ultimoPedido.current);
    }
  }, [disparar]);

  const reiniciar = useCallback(() => {
    if (!emAndamento.current) {
      reset();
    }
  }, [reset]);

  const erro = mutacao.error ? paraAppError(mutacao.error) : null;
  const estado: EstadoDeEnvio = mutacao.isPending
    ? 'enviando'
    : mutacao.isSuccess
      ? 'salvo'
      : mutacao.isError
        ? 'falhou'
        : 'parado';

  return {
    estado,
    enviando: estado === 'enviando',
    salvo: estado === 'salvo',
    falhou: estado === 'falhou',
    erro,
    semResposta: erro?.semResposta ?? false,
    resposta: mutacao.data,
    enviar: disparar,
    tentarDeNovo,
    reiniciar,
  };
}
