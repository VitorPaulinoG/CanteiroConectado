import type { ReactNode } from 'react';

import { useSessao } from './SessaoContext';
import type { Perfil, UsuarioSessao } from './tipos';

export type Restricao = { perfis?: Perfil[]; administrador?: boolean };

export function permite(usuario: UsuarioSessao, { perfis, administrador }: Restricao): boolean {
  const perfilPermitido = !perfis || perfis.includes(usuario.perfil);
  const administradorPermitido = !administrador || usuario.administrador;
  return perfilPermitido && administradorPermitido;
}

export function usePermite(restricao: Restricao): boolean {
  const { estado } = useSessao();
  return estado.status === 'autenticado' && permite(estado.usuario, restricao);
}

type Props = Restricao & {
  children: ReactNode;
  alternativa?: ReactNode;
};

export function SomentePerfil({ perfis, administrador, children, alternativa = null }: Props) {
  return usePermite({ perfis, administrador }) ? children : alternativa;
}
