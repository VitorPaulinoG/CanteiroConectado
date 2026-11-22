import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { QueryClient } from '@tanstack/react-query';

import { chavesEstoque } from '@/shared/estoque';
import { erroDaApi } from '@/test-utils/api';
import { criarQueryClientDeTeste, renderizar } from '@/test-utils/render';
import { requisicaoDaCamila } from '@/test-utils/requisicoes';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import {
  atenderRequisicao,
  buscarRequisicao,
  cancelarRequisicao,
  chavesRequisicoes,
  type Requisicao,
} from '../api';
import { DetalheRequisicaoTela } from '../telas/DetalheRequisicaoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({
  ...jest.requireActual('../api'),
  buscarRequisicao: jest.fn(),
  atenderRequisicao: jest.fn(),
  cancelarRequisicao: jest.fn(),
}));
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: jest.fn() }),
  useFocusEffect: jest.fn(),
}));

const aprovada: Requisicao = {
  ...requisicaoDaCamila,
  status: 'APROVADA',
  decididoPorNome: 'Sebastião Lima',
  acoes: { decidir: false, atender: true, cancelar: false },
};

async function abrir(requisicao: Requisicao, usuario: UsuarioSessao, queryClient?: QueryClient) {
  jest.mocked(buscarRequisicao).mockResolvedValue(requisicao);
  await renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <DetalheRequisicaoTela id={requisicao.id} />
    </ComSessao>,
    queryClient,
  );
}

describe('Atender e cancelar em S14', () => {
  beforeEach(() => jest.clearAllMocks());

  it('pede confirmação, marca como atendida e atualiza as listas e o Início', async () => {
    const queryClient = criarQueryClientDeTeste();
    queryClient.setQueryData(chavesRequisicoes.lista('c-aurora', 'minhas'), { pages: [] });
    queryClient.setQueryData(chavesEstoque.resumo('c-aurora'), { zerados: 0 });
    const atendida = {
      ...aprovada,
      status: 'ATENDIDA' as const,
      acoes: { decidir: false, atender: false, cancelar: false },
    };
    jest.mocked(atenderRequisicao).mockResolvedValue(atendida);
    await abrir(aprovada, almoxarife, queryClient);

    await fireEvent.press(await screen.findByText('Marcar como atendida'));
    expect(screen.getByText('Marcar como atendida?')).toBeTruthy();
    expect(atenderRequisicao).not.toHaveBeenCalled();
    jest.mocked(buscarRequisicao).mockResolvedValue(atendida);
    const botoes = screen.getAllByText('Marcar como atendida');
    await fireEvent.press(botoes[botoes.length - 1]);

    await waitFor(() => expect(atenderRequisicao).toHaveBeenCalledWith('r-1'));
    expect(await screen.findByText('Atendida')).toBeTruthy();
    expect(
      queryClient.getQueryState(chavesRequisicoes.lista('c-aurora', 'minhas'))?.isInvalidated,
    ).toBe(true);
    expect(queryClient.getQueryState(chavesEstoque.resumo('c-aurora'))?.isInvalidated).toBe(true);
  });

  it('cancela a própria pendente depois da confirmação', async () => {
    jest.mocked(cancelarRequisicao).mockResolvedValue({
      ...requisicaoDaCamila,
      status: 'CANCELADA',
      acoes: { decidir: false, atender: false, cancelar: false },
    });
    await abrir(requisicaoDaCamila, gestor);

    await fireEvent.press(await screen.findByText('Cancelar requisição'));
    expect(screen.getByText('Cancelar a requisição?')).toBeTruthy();
    await fireEvent.press(screen.getByText('Voltar'));
    expect(cancelarRequisicao).not.toHaveBeenCalled();

    await fireEvent.press(screen.getByText('Cancelar requisição'));
    const botoes = screen.getAllByText('Cancelar requisição');
    await fireEvent.press(botoes[botoes.length - 1]);
    await waitFor(() => expect(cancelarRequisicao).toHaveBeenCalledWith('r-1'));
  });

  it('mostra a orientação da API quando a requisição já foi aprovada', async () => {
    jest.mocked(cancelarRequisicao).mockRejectedValue(
      erroDaApi(409, {
        codigo: 'REQUISICAO_NAO_PENDENTE',
        detail:
          'Esta requisição já foi aprovada por Sebastião Lima. Para desistir, fale com quem aprovou.',
      }),
    );
    await abrir(requisicaoDaCamila, gestor);

    await fireEvent.press(await screen.findByText('Cancelar requisição'));
    const botoes = screen.getAllByText('Cancelar requisição');
    await fireEvent.press(botoes[botoes.length - 1]);

    expect(
      await screen.findByText(
        'Esta requisição já foi aprovada por Sebastião Lima. Para desistir, fale com quem aprovou.',
      ),
    ).toBeTruthy();
  });

  it('não mostra ações quando a requisição já terminou', async () => {
    await abrir(
      {
        ...aprovada,
        status: 'ATENDIDA',
        acoes: { decidir: false, atender: false, cancelar: false },
      },
      almoxarife,
    );
    await screen.findByText('Atendida');
    expect(screen.queryByText('Marcar como atendida')).toBeNull();
    expect(screen.queryByText('Cancelar requisição')).toBeNull();
  });
});
