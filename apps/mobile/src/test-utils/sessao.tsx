import type { ReactNode } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import type { Canteiro, ValorCanteiroAtivo } from '@/shared/canteiro';
import { CanteiroAtivoContext } from '@/shared/canteiro/CanteiroAtivoContext';
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

export const aurora: Canteiro = {
  id: 'c-aurora',
  nome: 'Residencial Aurora',
  endereco: 'Rua das Obras, 100',
  ativo: true,
};

export function valorCanteiroAtivo(
  sobrescrever: Partial<ValorCanteiroAtivo> = {},
): ValorCanteiroAtivo {
  return {
    status: 'pronto',
    canteiroId: aurora.id,
    canteiro: aurora,
    podeEscolher: false,
    escolher: jest.fn(async () => undefined),
    tentarDeNovo: jest.fn(),
    ...sobrescrever,
  };
}

type PropsComSessao = {
  valor: ValorSessao;
  canteiro?: ValorCanteiroAtivo;
  children: ReactNode;
};

export function ComSessao({ valor, canteiro, children }: PropsComSessao) {
  const usuario = valor.estado.status === 'autenticado' ? valor.estado.usuario : null;
  const canteiroAtivo =
    canteiro ?? valorCanteiroAtivo({ podeEscolher: usuario?.perfil === 'GESTOR' });
  return (
    <SafeAreaProvider
      initialMetrics={{
        frame: { x: 0, y: 0, width: 360, height: 640 },
        insets: { top: 0, left: 0, right: 0, bottom: 0 },
      }}
    >
      <SessaoContext.Provider value={valor}>
        <CanteiroAtivoContext.Provider value={canteiroAtivo}>
          {children}
        </CanteiroAtivoContext.Provider>
      </SessaoContext.Provider>
    </SafeAreaProvider>
  );
}
