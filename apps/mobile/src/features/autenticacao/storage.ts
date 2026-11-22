import * as SecureStore from 'expo-secure-store';

import type { UsuarioSessao } from '@/shared/sessao';

export type SessaoSalva = {
  token: string;
  expiraEm: string;
  usuario: UsuarioSessao;
};

export const CHAVE_SESSAO = 'canteiro.sessao';

export async function salvarSessao(sessao: SessaoSalva): Promise<void> {
  await SecureStore.setItemAsync(CHAVE_SESSAO, JSON.stringify(sessao));
}

export async function lerSessao(): Promise<SessaoSalva | null> {
  const texto = await SecureStore.getItemAsync(CHAVE_SESSAO);
  if (!texto) {
    return null;
  }
  try {
    const sessao = JSON.parse(texto) as SessaoSalva;
    return sessao.token && sessao.expiraEm && sessao.usuario ? sessao : null;
  } catch {
    return null;
  }
}

export async function apagarSessao(): Promise<void> {
  await SecureStore.deleteItemAsync(CHAVE_SESSAO);
}

export function sessaoExpirada(sessao: SessaoSalva, agora = Date.now()): boolean {
  return new Date(sessao.expiraEm).getTime() <= agora;
}
