import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { erroDaApi, erroDeRede, erroDeTempo } from '@/test-utils/api';

import { useEnvio } from '../envio';
import { criarQueryClient } from '../queryClient';

type Pedido = { id: string; quantidade: number };

function montar(mutationFn: (pedido: Pedido) => Promise<string>) {
  const queryClient = criarQueryClient();
  queryClient.setDefaultOptions({
    ...queryClient.getDefaultOptions(),
    mutations: { ...queryClient.getDefaultOptions().mutations, retryDelay: 0 },
  });
  function Envoltorio({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }
  return renderHook(() => useEnvio({ mutationKey: ['teste'], mutationFn }), {
    wrapper: Envoltorio,
  });
}

const pedido: Pedido = { id: 'id-fixo', quantidade: 1 };

describe('useEnvio', () => {
  it('passa por enviando e só fica salvo depois da resposta de sucesso', async () => {
    let responder: (valor: string) => void = () => undefined;
    const mutationFn = jest.fn(() => new Promise<string>((resolver) => (responder = resolver)));
    const { result } = await montar(mutationFn);
    expect(result.current.estado).toBe('parado');

    await act(async () => result.current.enviar(pedido));
    await waitFor(() => expect(result.current.enviando).toBe(true));
    expect(result.current.salvo).toBe(false);

    await act(async () => responder('ok'));
    await waitFor(() => expect(result.current.salvo).toBe(true));
    expect(result.current.resposta).toBe('ok');
  });

  it('tenta mais uma vez sozinho quando não há resposta e depois mostra a falha', async () => {
    const mutationFn = jest.fn().mockRejectedValue(erroDeRede());
    const { result } = await montar(mutationFn);

    await act(async () => result.current.enviar(pedido));
    await waitFor(() => expect(result.current.falhou).toBe(true));

    expect(mutationFn).toHaveBeenCalledTimes(2);
    expect(result.current.semResposta).toBe(true);
    expect(result.current.erro?.codigo).toBe('SEM_CONEXAO');
  });

  it('reenvia o mesmo pedido, com o mesmo id, no Tentar de novo', async () => {
    const mutationFn = jest
      .fn()
      .mockRejectedValueOnce(erroDeTempo())
      .mockRejectedValueOnce(erroDeTempo())
      .mockResolvedValue('ok');
    const { result } = await montar(mutationFn);

    await act(async () => result.current.enviar(pedido));
    await waitFor(() => expect(result.current.falhou).toBe(true));
    await act(async () => result.current.tentarDeNovo());
    await waitFor(() => expect(result.current.salvo).toBe(true));

    expect(mutationFn).toHaveBeenCalledTimes(3);
    for (const chamada of mutationFn.mock.calls) {
      expect(chamada[0]).toEqual({ id: 'id-fixo', quantidade: 1 });
    }
  });

  it('não repete quando a API responde 4xx e guarda o erro para mostrar no campo', async () => {
    const mutationFn = jest
      .fn()
      .mockRejectedValue(erroDaApi(422, { codigo: 'SALDO_INSUFICIENTE', campo: 'quantidade' }));
    const { result } = await montar(mutationFn);

    await act(async () => result.current.enviar(pedido));
    await waitFor(() => expect(result.current.falhou).toBe(true));

    expect(mutationFn).toHaveBeenCalledTimes(1);
    expect(result.current.semResposta).toBe(false);
    expect(result.current.erro?.codigo).toBe('SALDO_INSUFICIENTE');
  });

  it('dispara um único envio no toque duplo', async () => {
    const mutationFn = jest.fn(() => new Promise<string>(() => undefined));
    const { result } = await montar(mutationFn);

    await act(async () => {
      result.current.enviar(pedido);
      result.current.enviar(pedido);
    });

    expect(mutationFn).toHaveBeenCalledTimes(1);
  });
});
