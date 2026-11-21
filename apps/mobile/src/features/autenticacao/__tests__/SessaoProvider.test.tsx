import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClientProvider } from '@tanstack/react-query';
import * as SecureStore from 'expo-secure-store';
import type { ReactNode } from 'react';

import { AxiosError } from 'axios';

import { api } from '@/shared/api';
import { useSessao, type ValorSessao } from '@/shared/sessao';
import { criarQueryClientDeTeste } from '@/test-utils/render';

import { buscarEu, postarLogin } from '../api';
import { SessaoProvider } from '../SessaoProvider';
import { CHAVE_SESSAO, lerSessao } from '../storage';

jest.mock('../api', () => ({ postarLogin: jest.fn(), buscarEu: jest.fn() }));

const usuario = {
  id: '1',
  nome: 'Camila',
  email: 'camila@obra.com',
  perfil: 'GESTOR' as const,
  administrador: false,
  canteiroId: null,
  precisaTrocarSenha: false,
};

let resultado: { current: ValorSessao };

function Envoltorio({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={criarQueryClientDeTeste()}>
      <SessaoProvider>{children}</SessaoProvider>
    </QueryClientProvider>
  );
}

async function montar() {
  const montado = await renderHook(() => useSessao(), { wrapper: Envoltorio });
  resultado = montado.result;
}

async function aguardarStatus(status: ValorSessao['estado']['status']) {
  await waitFor(() => expect(resultado.current.estado.status).toBe(status));
}

describe('SessaoProvider', () => {
  beforeEach(() => {
    (SecureStore as unknown as { __limpar: () => void }).__limpar();
    jest.mocked(buscarEu).mockResolvedValue(usuario);
  });

  it('começa anônimo quando não há sessão guardada', async () => {
    await montar();
    await aguardarStatus('anonimo');
  });

  it('restaura a sessão guardada e atualiza os dados com /eu', async () => {
    await SecureStore.setItemAsync(
      CHAVE_SESSAO,
      JSON.stringify({ token: 't', expiraEm: '2999-01-01T00:00:00Z', usuario }),
    );
    jest.mocked(buscarEu).mockResolvedValue({ ...usuario, nome: 'Camila Souza' });

    await montar();

    await aguardarStatus('autenticado');
    await waitFor(() =>
      expect(
        resultado.current.estado.status === 'autenticado' && resultado.current.estado.usuario.nome,
      ).toBe('Camila Souza'),
    );
  });

  it('descarta sessão expirada e avisa para entrar de novo', async () => {
    await SecureStore.setItemAsync(
      CHAVE_SESSAO,
      JSON.stringify({ token: 't', expiraEm: '2000-01-01T00:00:00Z', usuario }),
    );

    await montar();

    await aguardarStatus('anonimo');
    expect(resultado.current.estado).toEqual({
      status: 'anonimo',
      aviso: 'Entre de novo para continuar.',
    });
    expect(await lerSessao()).toBeNull();
  });

  it('entra, guarda o token e sai apagando o token', async () => {
    jest.mocked(postarLogin).mockResolvedValue({
      token: 'novo',
      expiraEm: '2999-01-01T00:00:00Z',
      usuario,
    });
    await montar();
    await aguardarStatus('anonimo');

    await act(() => resultado.current.entrar('camila@obra.com', 'senha123'));
    expect(resultado.current.estado.status).toBe('autenticado');
    expect((await lerSessao())?.token).toBe('novo');

    await act(() => resultado.current.sair());
    expect(resultado.current.estado.status).toBe('anonimo');
    expect(await lerSessao()).toBeNull();
  });

  it('volta para o login com aviso quando a API responde 401', async () => {
    jest.mocked(postarLogin).mockResolvedValue({
      token: 'novo',
      expiraEm: '2999-01-01T00:00:00Z',
      usuario,
    });
    await montar();
    await aguardarStatus('anonimo');
    await act(() => resultado.current.entrar('camila@obra.com', 'senha123'));

    await act(async () => {
      await api
        .get('/api/v1/itens', {
          adapter: async (config) => {
            throw new AxiosError(
              'x',
              'ERR',
              config,
              {},
              {
                status: 401,
                statusText: '',
                headers: {},
                config,
                data: { codigo: 'NAO_AUTENTICADO' },
              },
            );
          },
        })
        .catch(() => undefined);
    });

    expect(resultado.current.estado).toEqual({
      status: 'anonimo',
      aviso: 'Entre de novo para continuar.',
    });
    expect(await lerSessao()).toBeNull();
  });

  it('envia o token da sessão nas requisições', async () => {
    jest.mocked(postarLogin).mockResolvedValue({
      token: 'token-da-sessao',
      expiraEm: '2999-01-01T00:00:00Z',
      usuario,
    });
    await montar();
    await aguardarStatus('anonimo');
    await act(() => resultado.current.entrar('camila@obra.com', 'senha123'));

    let enviado: string | undefined;
    await api.get('/api/v1/itens', {
      adapter: async (config) => {
        enviado = String(config.headers.get('Authorization'));
        return { data: {}, status: 200, statusText: 'OK', headers: {}, config };
      },
    });
    expect(enviado).toBe('Bearer token-da-sessao');
  });
});
