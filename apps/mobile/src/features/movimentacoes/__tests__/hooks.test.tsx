import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { chavesEstoque } from '@/shared/estoque';
import { saidaGravada } from '@/test-utils/estoque';
import { criarQueryClientDeTeste } from '@/test-utils/render';

import { registrarMovimentacao } from '../api';
import { useRegistrarMovimentacao } from '../hooks';

jest.mock('../api', () => ({ registrarMovimentacao: jest.fn() }));

describe('useRegistrarMovimentacao', () => {
  it('invalida o estoque, o detalhe do item e o resumo do Início depois do sucesso', async () => {
    jest.mocked(registrarMovimentacao).mockResolvedValue(saidaGravada);
    const queryClient = criarQueryClientDeTeste();
    const chaves = [
      chavesEstoque.lista('c-aurora', {}),
      chavesEstoque.detalhe('c-aurora', 'i-furadeira'),
      chavesEstoque.resumo('c-aurora'),
      ['movimentacoes', 'historico'],
    ];
    const outroCanteiro = chavesEstoque.resumo('c-outro');
    for (const chave of [...chaves, outroCanteiro]) {
      queryClient.setQueryData(chave, { qualquer: true });
    }
    function Envoltorio({ children }: { children: ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    const { result } = await renderHook(() => useRegistrarMovimentacao('c-aurora'), {
      wrapper: Envoltorio,
    });

    await act(async () =>
      result.current.enviar({
        id: 'id-1',
        tipo: 'SAIDA',
        motivo: 'RETIRADA',
        itemId: 'i-furadeira',
        quantidade: 1,
      }),
    );
    await waitFor(() => expect(result.current.salvo).toBe(true));

    for (const chave of chaves) {
      expect(queryClient.getQueryState(chave)?.isInvalidated).toBe(true);
    }
    expect(queryClient.getQueryState(outroCanteiro)?.isInvalidated).toBe(false);
  });
});
