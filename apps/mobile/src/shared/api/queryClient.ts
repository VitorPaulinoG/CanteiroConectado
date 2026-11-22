import { QueryClient } from '@tanstack/react-query';

import { paraAppError } from './errors';

export const TEMPO_DE_DADO_FRESCO_MS = 30_000;
export const TENTATIVAS_EXTRAS_CONSULTA = 2;
export const TENTATIVAS_EXTRAS_MUTACAO = 1;

export function deveRepetirConsulta(falhas: number, erro: unknown): boolean {
  if (paraAppError(erro).erroDoCliente) {
    return false;
  }
  return falhas < TENTATIVAS_EXTRAS_CONSULTA;
}

export function deveRepetirMutacao(falhas: number, erro: unknown): boolean {
  return paraAppError(erro).semResposta && falhas < TENTATIVAS_EXTRAS_MUTACAO;
}

export function esperaCrescente(tentativa: number): number {
  return Math.min(1_000 * 2 ** tentativa, 8_000);
}

export function criarQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: TEMPO_DE_DADO_FRESCO_MS,
        retry: deveRepetirConsulta,
        retryDelay: esperaCrescente,
      },
      mutations: {
        retry: deveRepetirMutacao,
        retryDelay: esperaCrescente,
      },
    },
  });
}
