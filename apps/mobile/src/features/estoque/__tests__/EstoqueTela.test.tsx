import { act, fireEvent, screen } from '@testing-library/react-native';

import { listarEstoque, type ItemEstoque } from '@/shared/estoque/api';
import { renderizar } from '@/test-utils/render';
import { autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { EstoqueTela } from '../telas/EstoqueTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  listarEstoque: jest.fn(),
}));

const mockPush = jest.fn();
let mockParametros: { filtro?: string } = {};
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush, back: jest.fn() }),
  useLocalSearchParams: () => mockParametros,
}));

const cimento: ItemEstoque = {
  itemId: 'i-cimento',
  codigo: 'MAT-0001',
  descricao: 'Cimento CP-II 50kg',
  unidade: 'SACO',
  unidadeSimbolo: 'saco',
  tipo: 'MATERIAL',
  quantidadeMinima: 50,
  saldo: 30,
  status: 'ABAIXO_DO_MINIMO',
};

function pagina(conteudo: ItemEstoque[], numero = 0, totalPaginas = 1) {
  return { conteudo, pagina: numero, tamanho: 20, totalElementos: conteudo.length, totalPaginas };
}

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(gestor))}>
      <EstoqueTela />
    </ComSessao>,
  );
}

describe('EstoqueTela (S06)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
    mockParametros = {};
  });
  afterEach(() => jest.useRealTimers());

  it('lista saldo, unidade e selo do canteiro ativo e abre o detalhe', async () => {
    jest.mocked(listarEstoque).mockResolvedValue(pagina([cimento]));
    await montar();

    expect(await screen.findByText('Cimento CP-II 50kg')).toBeTruthy();
    expect(screen.getByText('MAT-0001 · 30 saco')).toBeTruthy();
    expect(screen.getAllByText('Abaixo do mínimo')).toHaveLength(2);
    expect(listarEstoque).toHaveBeenCalledWith('c-aurora', { q: undefined, status: undefined }, 0);

    await fireEvent.press(screen.getByText('Cimento CP-II 50kg'));
    expect(mockPush).toHaveBeenCalledWith('/estoque/i-cimento');
  });

  it('filtra abaixo do mínimo com os status de atenção e crítico', async () => {
    jest.mocked(listarEstoque).mockResolvedValue(pagina([]));
    await montar();
    await screen.findByText('Nenhum item no catálogo');

    await fireEvent.press(screen.getAllByText('Abaixo do mínimo')[0]);

    expect(listarEstoque).toHaveBeenLastCalledWith(
      'c-aurora',
      { q: undefined, status: ['ZERADO', 'ABAIXO_DO_MINIMO'] },
      0,
    );
    expect(await screen.findByText('Tudo certo por aqui.')).toBeTruthy();
  });

  it('abre já filtrada quando vem do alerta do Início', async () => {
    mockParametros = { filtro: 'alerta' };
    jest.mocked(listarEstoque).mockResolvedValue(pagina([cimento]));
    await montar();

    await screen.findByText('Cimento CP-II 50kg');
    expect(listarEstoque).toHaveBeenCalledWith(
      'c-aurora',
      { q: undefined, status: ['ZERADO', 'ABAIXO_DO_MINIMO'] },
      0,
    );
  });

  it('busca depois da pausa e carrega a próxima página no fim da lista', async () => {
    jest
      .mocked(listarEstoque)
      .mockResolvedValueOnce(pagina([cimento], 0, 2))
      .mockResolvedValueOnce(pagina([{ ...cimento, itemId: 'i2', descricao: 'Areia' }], 1, 2))
      .mockResolvedValue(pagina([cimento]));
    await montar();
    await screen.findByText('Cimento CP-II 50kg');

    await fireEvent(screen.getByTestId('lista-do-estoque'), 'endReached');
    expect(await screen.findByText('Areia')).toBeTruthy();

    await fireEvent.changeText(screen.getByLabelText('Buscar no estoque'), 'cim');
    await act(async () => {
      jest.advanceTimersByTime(300);
    });
    expect(listarEstoque).toHaveBeenLastCalledWith('c-aurora', { q: 'cim', status: undefined }, 0);
  });
});
