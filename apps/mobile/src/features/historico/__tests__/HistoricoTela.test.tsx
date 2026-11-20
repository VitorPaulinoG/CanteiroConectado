import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { listarColaboradores } from '@/shared/colaborador/api';
import { listarItens } from '@/shared/item/api';
import { joao, pagina, saidaGravada } from '@/test-utils/estoque';
import { criarQueryClientDeTeste, renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { listarHistorico } from '../api';
import { HistoricoTela } from '../telas/HistoricoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), listarHistorico: jest.fn() }));
jest.mock('@/shared/item/api', () => ({
  ...jest.requireActual('@/shared/item/api'),
  listarItens: jest.fn(),
}));
jest.mock('@/shared/colaborador/api', () => ({
  ...jest.requireActual('@/shared/colaborador/api'),
  listarColaboradores: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useFocusEffect: jest.fn(),
}));

const hoje = new Date();
const ajusteDeHoje = {
  ...saidaGravada,
  id: 'm-ajuste',
  motivo: 'AJUSTE' as const,
  itemDescricao: 'Cimento CP-II 50kg',
  unidadeSimbolo: 'saco',
  colaboradorNome: null,
  frenteObra: null,
  criadoEm: hoje.toISOString(),
};
const saidaAntiga = { ...saidaGravada, criadoEm: '2026-10-20T15:00:00Z' };

function montar(usuario: UsuarioSessao = gestor, queryClient = criarQueryClientDeTeste()) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <HistoricoTela />
    </ComSessao>,
    queryClient,
  );
}

describe('HistoricoTela (S16)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.mocked(listarHistorico).mockResolvedValue(pagina([ajusteDeHoje, saidaAntiga]));
  });

  it('agrupa por dia, mostra item, autor, colaborador e o selo de ajuste e abre o registro', async () => {
    await montar();

    expect(await screen.findByText('20/10/2026')).toBeTruthy();
    expect(screen.getAllByText('Hoje')).toHaveLength(2);
    expect(screen.getByText('Cimento CP-II 50kg · −1 saco')).toBeTruthy();
    expect(screen.getByText('Furadeira de impacto 700W · −1 un')).toBeTruthy();
    expect(screen.getByText(/Retirada · João da Silva · Fundação/)).toBeTruthy();
    expect(screen.getAllByText('Ajuste').length).toBeGreaterThan(0);
    expect(listarHistorico).toHaveBeenCalledWith(
      'c-aurora',
      {
        de: undefined,
        ate: undefined,
        tipo: undefined,
        itemId: undefined,
        colaboradorId: undefined,
      },
      0,
    );

    await fireEvent.press(screen.getByText('Furadeira de impacto 700W · −1 un'));
    expect(mockPush).toHaveBeenCalledWith('/movimentacao/m-saida');
  });

  it('filtra por hoje com as datas em UTC e limpa os filtros', async () => {
    await montar();
    await screen.findByText('20/10/2026');

    await fireEvent.press(screen.getAllByText('Hoje')[0]);

    await waitFor(() =>
      expect(listarHistorico).toHaveBeenLastCalledWith(
        'c-aurora',
        expect.objectContaining({
          de: expect.stringMatching(/Z$/),
          ate: expect.stringMatching(/Z$/),
        }),
        0,
      ),
    );
    await fireEvent.press(screen.getByText('Limpar filtros'));
    await waitFor(() =>
      expect(listarHistorico).toHaveBeenLastCalledWith(
        'c-aurora',
        expect.objectContaining({ de: undefined, ate: undefined }),
        0,
      ),
    );
  });

  it('filtra por tipo, item e colaborador pelo modal, sem oferecer cadastro de colaborador', async () => {
    jest.mocked(listarItens).mockResolvedValue(
      pagina([
        {
          id: 'i-furadeira',
          codigo: 'FER-0012',
          descricao: 'Furadeira de impacto 700W',
          unidade: 'UN',
          unidadeSimbolo: 'un',
          tipo: 'FERRAMENTA',
          quantidadeMinima: 1,
          ativo: false,
        },
      ]),
    );
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([joao]));
    await montar();
    await screen.findByText('20/10/2026');

    await fireEvent.press(screen.getByText('Filtros'));
    await fireEvent.press(screen.getByText('Saídas'));
    await fireEvent.press(screen.getByLabelText('Escolher item'));
    await fireEvent.press(await screen.findByText('Furadeira de impacto 700W'));
    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));
    expect(screen.queryByText('Novo colaborador')).toBeNull();
    await fireEvent.press(await screen.findByText('João da Silva'));
    await fireEvent.press(screen.getByText('Pronto'));

    await waitFor(() =>
      expect(listarHistorico).toHaveBeenLastCalledWith(
        'c-aurora',
        expect.objectContaining({
          tipo: 'SAIDA',
          itemId: 'i-furadeira',
          colaboradorId: 'col-joao',
        }),
        0,
      ),
    );
    expect(listarItens).toHaveBeenCalledWith({ q: undefined, ativo: undefined }, 0);
    expect(screen.getByText('Filtros (3)')).toBeTruthy();
  });

  it('mantém os filtros durante a sessão ao sair e voltar para a tela', async () => {
    const queryClient = criarQueryClientDeTeste();
    const primeira = await montar(gestor, queryClient);
    await screen.findByText('20/10/2026');
    await fireEvent.press(screen.getByText('7 dias'));
    await primeira.unmount();

    await montar(gestor, queryClient);

    await screen.findByText('Limpar filtros');
    expect(listarHistorico).toHaveBeenLastCalledWith(
      'c-aurora',
      expect.objectContaining({ de: expect.any(String) }),
      0,
    );
  });

  it('mostra o vazio por filtro', async () => {
    jest.mocked(listarHistorico).mockResolvedValue(pagina([]));
    await montar();
    expect(await screen.findByText('Nenhuma movimentação')).toBeTruthy();
    await fireEvent.press(screen.getByText('7 dias'));
    expect(await screen.findByText('Nenhuma movimentação com estes filtros')).toBeTruthy();
  });

  it('mostra acesso negado ao ALMOXARIFE', async () => {
    await montar(almoxarife);
    expect(screen.getByText('Você não tem acesso a esta área')).toBeTruthy();
    expect(listarHistorico).not.toHaveBeenCalled();
  });
});
