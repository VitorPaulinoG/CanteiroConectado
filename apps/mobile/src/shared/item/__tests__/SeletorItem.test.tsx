import { act, fireEvent, screen } from '@testing-library/react-native';
import { useState } from 'react';

import { listarEstoque, type ItemEstoque } from '@/shared/estoque/api';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { listarItens, type Item } from '../api';
import { SeletorItem, type ItemSelecionado } from '../SeletorItem';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), listarItens: jest.fn() }));
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  listarEstoque: jest.fn(),
}));

const furadeira: ItemEstoque = {
  itemId: 'i-furadeira',
  codigo: 'FER-0012',
  descricao: 'Furadeira de impacto 700W',
  unidade: 'UN',
  unidadeSimbolo: 'un',
  tipo: 'FERRAMENTA',
  quantidadeMinima: 1,
  saldo: 2,
  status: 'NORMAL',
};
const martelo: ItemEstoque = {
  ...furadeira,
  itemId: 'i-martelo',
  codigo: 'FER-0020',
  descricao: 'Martelo de unha',
  saldo: 0,
  status: 'ZERADO',
};

function pagina<T>(conteudo: T[]) {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}

const aoMudar = jest.fn();

function Campo(props: { modo: 'catalogo' } | { modo: 'estoque'; exigirSaldo?: boolean }) {
  const [valor, setValor] = useState<ItemSelecionado | null>(null);
  const modo =
    props.modo === 'estoque'
      ? { modo: 'estoque' as const, canteiroId: 'c-aurora', exigirSaldo: props.exigirSaldo }
      : { modo: 'catalogo' as const };
  return (
    <SeletorItem
      {...modo}
      valor={valor}
      aoMudar={(item) => {
        aoMudar(item);
        setValor(item);
      }}
    />
  );
}

function montar(props: Parameters<typeof Campo>[0]) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <Campo {...props} />
    </ComSessao>,
  );
}

describe('SeletorItem (M1)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
  });
  afterEach(() => jest.useRealTimers());

  it('mostra o saldo e o selo no modo estoque e escolhe o item', async () => {
    jest.mocked(listarEstoque).mockResolvedValue(pagina([furadeira]));
    await montar({ modo: 'estoque', exigirSaldo: true });

    await fireEvent.press(screen.getByLabelText('Escolher item'));
    expect(await screen.findByText('FER-0012 · Saldo 2 un')).toBeTruthy();
    expect(screen.getByText('Normal')).toBeTruthy();
    await fireEvent.press(screen.getByText('Furadeira de impacto 700W'));

    expect(aoMudar).toHaveBeenCalledWith(
      expect.objectContaining({ itemId: 'i-furadeira', saldo: 2 }),
    );
    expect(screen.getByText('Saldo no canteiro: 2 un')).toBeTruthy();
    expect(listarEstoque).toHaveBeenCalledWith('c-aurora', { q: undefined }, 0);
  });

  it('desabilita o item sem saldo quando a saída exige saldo', async () => {
    jest.mocked(listarEstoque).mockResolvedValue(pagina([martelo]));
    await montar({ modo: 'estoque', exigirSaldo: true });

    await fireEvent.press(screen.getByLabelText('Escolher item'));
    await fireEvent.press(await screen.findByText('Martelo de unha'));

    expect(aoMudar).not.toHaveBeenCalled();
    expect(screen.getByText('Zerado')).toBeTruthy();
  });

  it('busca no catálogo só itens ativos depois da pausa de 300 ms', async () => {
    const cimento: Item = {
      id: 'i-cimento',
      codigo: 'MAT-0001',
      descricao: 'Cimento CP-II',
      unidade: 'SACO',
      unidadeSimbolo: 'saco',
      tipo: 'MATERIAL',
      quantidadeMinima: 50,
      ativo: true,
    };
    jest.mocked(listarItens).mockResolvedValue(pagina([cimento]));
    await montar({ modo: 'catalogo' });

    await fireEvent.press(screen.getByLabelText('Escolher item'));
    await fireEvent.changeText(screen.getByLabelText('Buscar item'), 'cim');
    expect(listarItens).not.toHaveBeenCalledWith({ q: 'cim', ativo: true }, 0);
    await act(async () => {
      jest.advanceTimersByTime(300);
    });
    expect(listarItens).toHaveBeenLastCalledWith({ q: 'cim', ativo: true }, 0);

    await fireEvent.press(await screen.findByText('Cimento CP-II'));
    expect(aoMudar).toHaveBeenCalledWith({
      itemId: 'i-cimento',
      codigo: 'MAT-0001',
      descricao: 'Cimento CP-II',
      unidadeSimbolo: 'saco',
      tipo: 'MATERIAL',
    });
  });
});
