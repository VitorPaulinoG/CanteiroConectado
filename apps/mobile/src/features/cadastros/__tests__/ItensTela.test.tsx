import { act, fireEvent, screen } from '@testing-library/react-native';

import { listarItens, type Item } from '@/shared/item/api';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { ItensTela } from '../telas/ItensTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/item/api', () => ({
  ...jest.requireActual('@/shared/item/api'),
  listarItens: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush, back: jest.fn() }) }));

const furadeira: Item = {
  id: 'i-furadeira',
  codigo: 'FER-0012',
  descricao: 'Furadeira de impacto 700W',
  unidade: 'UN',
  unidadeSimbolo: 'un',
  tipo: 'FERRAMENTA',
  quantidadeMinima: 1,
  ativo: true,
};

function pagina(conteudo: Item[], numero = 0, totalPaginas = 1) {
  return { conteudo, pagina: numero, tamanho: 20, totalElementos: conteudo.length, totalPaginas };
}

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <ItensTela />
    </ComSessao>,
  );
}

describe('ItensTela (S20)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
  });
  afterEach(() => jest.useRealTimers());

  it('mostra código, descrição, tipo e unidade e abre o item', async () => {
    jest.mocked(listarItens).mockResolvedValue(pagina([furadeira]));
    await montar();

    expect(await screen.findByText('Furadeira de impacto 700W')).toBeTruthy();
    expect(screen.getByText('FER-0012 · Ferramenta · un')).toBeTruthy();
    expect(listarItens).toHaveBeenCalledWith({ q: undefined, tipo: undefined, ativo: true }, 0);

    await fireEvent.press(screen.getByText('Furadeira de impacto 700W'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/itens/i-furadeira');
  });

  it('busca depois da pausa e filtra por tipo e situação', async () => {
    jest.mocked(listarItens).mockResolvedValue(pagina([]));
    await montar();
    await screen.findByText('Nenhum item');

    await fireEvent.changeText(screen.getByLabelText('Buscar item'), 'furad');
    await act(async () => {
      jest.advanceTimersByTime(300);
    });
    await fireEvent.press(screen.getByText('Ferramentas'));
    await fireEvent.press(screen.getByText('Inativos'));

    expect(listarItens).toHaveBeenLastCalledWith(
      { q: 'furad', tipo: 'FERRAMENTA', ativo: false },
      0,
    );
  });

  it('carrega a próxima página ao chegar no fim da lista', async () => {
    jest
      .mocked(listarItens)
      .mockResolvedValueOnce(pagina([furadeira], 0, 2))
      .mockResolvedValueOnce(pagina([{ ...furadeira, id: 'i2', descricao: 'Martelo' }], 1, 2));
    await montar();
    await screen.findByText('Furadeira de impacto 700W');

    await fireEvent(screen.getByTestId('lista-de-itens'), 'endReached');

    expect(await screen.findByText('Martelo')).toBeTruthy();
    expect(listarItens).toHaveBeenLastCalledWith(expect.anything(), 1);
  });
});
