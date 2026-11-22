import { createContext, useContext } from 'react';

import type { UsuarioSessao, ValorSessao } from './tipos';

export const SessaoContext = createContext<ValorSessao | null>(null);

export function useSessao(): ValorSessao {
  const valor = useContext(SessaoContext);
  if (!valor) {
    throw new Error('useSessao precisa estar dentro de um SessaoProvider.');
  }
  return valor;
}

export function useUsuario(): UsuarioSessao {
  const { estado } = useSessao();
  if (estado.status !== 'autenticado') {
    throw new Error('useUsuario só pode ser usado na área autenticada.');
  }
  return estado.usuario;
}
