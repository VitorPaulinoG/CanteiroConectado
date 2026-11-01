import type { AppError } from './errors';

export function erroDoCampo(erro: AppError | null | undefined, campo: string): string | undefined {
  return erro && erro.extras.campo === campo ? erro.message : undefined;
}

export function erroSemCampo(
  erro: AppError | null | undefined,
  campos: string[],
): string | undefined {
  if (!erro) {
    return undefined;
  }
  const campo = erro.extras.campo;
  return typeof campo === 'string' && campos.includes(campo) ? undefined : erro.message;
}
