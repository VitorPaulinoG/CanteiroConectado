import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClientProvider } from '@tanstack/react-query';
import * as SecureStore from 'expo-secure-store';
import type { ReactNode } from 'react';

import { SessaoContext, type UsuarioSessao } from '@/shared/sessao';
import { criarQueryClientDeTeste } from '@/test-utils/render';
import { almoxarife, aurora, autenticado, gestor, valorSessao } from '@/test-utils/sessao';

import { buscarCanteiro, listarCanteiros } from '../api';
import { CanteiroAtivoProvider, useCanteiroAtivo } from '../CanteiroAtivoContext';
import { chaveDoCanteiroAtivo } from '../storage';

jest.mock('../api', () => ({
  ...jest.requireActual('../api'),
  buscarCanteiro: jest.fn(),
  listarCanteiros: jest.fn(),
}));

const sol = { id: 'c-sol', nome: 'Edifício Sol', endereco: null, ativo: true };

function montar(usuario: UsuarioSessao, queryClient = criarQueryClientDeTeste()) {
  function Envoltorio({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <SessaoContext.Provider value={valorSessao(autenticado(usuario))}>
          <CanteiroAtivoProvider>{children}</CanteiroAtivoProvider>
        </SessaoContext.Provider>
      </QueryClientProvider>
    );
  }
  return renderHook(() => useCanteiroAtivo(), { wrapper: Envoltorio });
}

describe('CanteiroAtivoProvider', () => {
  beforeEach(() => {
    (SecureStore as unknown as { __limpar: () => void }).__limpar();
    jest.mocked(buscarCanteiro).mockResolvedValue(aurora);
    jest.mocked(listarCanteiros).mockResolvedValue([aurora, sol]);
  });

  it('usa o canteiro do ALMOXARIFE sem deixar escolher', async () => {
    const { result } = await montar(almoxarife);
    expect(result.current.status).toBe('pronto');
    expect(result.current.canteiroId).toBe('c-aurora');
    expect(result.current.podeEscolher).toBe(false);
    await waitFor(() => expect(result.current.canteiro?.nome).toBe('Residencial Aurora'));
  });

  it('pede a escolha do GESTOR sem canteiro guardado', async () => {
    const { result } = await montar(gestor);
    await waitFor(() => expect(result.current.status).toBe('escolher'));
    expect(result.current.canteiroId).toBeNull();
  });

  it('guarda a escolha no SecureStore e invalida as outras consultas', async () => {
    const queryClient = criarQueryClientDeTeste();
    queryClient.setQueryData(['estoque', 'c-aurora'], { itens: [] });
    const { result } = await montar(gestor, queryClient);
    await waitFor(() => expect(result.current.status).toBe('escolher'));

    await act(() => result.current.escolher('c-sol'));

    expect(result.current.status).toBe('pronto');
    expect(result.current.canteiro?.nome).toBe('Edifício Sol');
    expect(await SecureStore.getItemAsync(chaveDoCanteiroAtivo(gestor.id))).toBe('c-sol');
    expect(queryClient.getQueryState(['estoque', 'c-aurora'])?.isInvalidated).toBe(true);
  });

  it('restaura a escolha guardada', async () => {
    await SecureStore.setItemAsync(chaveDoCanteiroAtivo(gestor.id), 'c-sol');
    const { result } = await montar(gestor);
    await waitFor(() => expect(result.current.status).toBe('pronto'));
    expect(result.current.canteiroId).toBe('c-sol');
  });

  it('volta a pedir a escolha quando o canteiro guardado foi desativado', async () => {
    await SecureStore.setItemAsync(chaveDoCanteiroAtivo(gestor.id), 'c-desativado');
    const { result } = await montar(gestor);
    await waitFor(() => expect(result.current.status).toBe('escolher'));
  });
});
