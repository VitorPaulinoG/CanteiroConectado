import { paraAppError } from '@/shared/api/errors';

export const AVISO_APOS_MS = 8_000;
export const LIMITE_MS = 90_000;
export const INTERVALO_ENTRE_TENTATIVAS_MS = 3_000;

type Opcoes = {
  chamar: () => Promise<unknown>;
  agora?: () => number;
  dormir?: (ms: number) => Promise<void>;
  limiteMs?: number;
  intervaloMs?: number;
};

const dormirPadrao = (ms: number) => new Promise<void>((resolver) => setTimeout(resolver, ms));

function valeRepetir(erro: unknown): boolean {
  const appError = paraAppError(erro);
  return appError.semResposta || (appError.status !== undefined && appError.status >= 500);
}

export async function acordarServidor({
  chamar,
  agora = Date.now,
  dormir = dormirPadrao,
  limiteMs = LIMITE_MS,
  intervaloMs = INTERVALO_ENTRE_TENTATIVAS_MS,
}: Opcoes): Promise<void> {
  const inicio = agora();
  for (;;) {
    try {
      await chamar();
      return;
    } catch (erro) {
      const decorrido = agora() - inicio;
      if (!valeRepetir(erro) || decorrido + intervaloMs >= limiteMs) {
        throw paraAppError(erro);
      }
      await dormir(intervaloMs);
    }
  }
}
