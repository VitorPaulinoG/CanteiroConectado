import type { ReactNode } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import {
  SessaoContext,
  type EstadoSessao,
  type UsuarioSessao,
  type ValorSessao,
} from '@/shared/sessao';

export const almoxarife: UsuarioSessao = {
  id: 'u-almoxarife',
  nome: 'Sebastião Lima',
  email: 'sebastiao@obra.com',
  perfil: 'ALMOXARIFE',
  administrador: false,
  canteiroId: 'c-aurora',
  precisaTrocarSenha: false,
};

export const gestor: UsuarioSessao = {
  id: 'u-gestor',
  nome: 'Camila Souza',
  email: 'camila@obra.com',
  perfil: 'GESTOR',
  administrador: false,
  canteiroId: null,
  precisaTrocarSenha: false,
};

export const administrador: UsuarioSessao = {
  ...gestor,
  id: 'u-admin',
  nome: 'Administrador',
  email: 'admin@obra.com',
  administrador: true,
};

export function valorSessao(
  estado: EstadoSessao,
  sobrescrever: Partial<ValorSessao> = {},
): ValorSessao {
  return {
    estado,
    entrar: jest.fn(),
    sair: jest.fn(),
    atualizarUsuario: jest.fn(),
    adiarTroca: jest.fn(),
    ...sobrescrever,
  };
}

export function autenticado(usuario: UsuarioSessao, trocaAdiada = false): EstadoSessao {
  return { status: 'autenticado', usuario, trocaAdiada };
}

export function ComSessao({ valor, children }: { valor: ValorSessao; children: ReactNode }) {
  return (
    <SafeAreaProvider
      initialMetrics={{
        frame: { x: 0, y: 0, width: 360, height: 640 },
        insets: { top: 0, left: 0, right: 0, bottom: 0 },
      }}
    >
      <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>
    </SafeAreaProvider>
  );
}
