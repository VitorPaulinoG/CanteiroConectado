import { fireEvent, screen } from '@testing-library/react-native';

import { buscarDetalheEstoque, type DetalheEstoque } from '@/shared/estoque/api';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { DetalheItemTela } from '../telas/DetalheItemTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  buscarDetalheEstoque: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush, back: jest.fn() }) }));

const furadeira: DetalheEstoque = {
  itemId: 'i-furadeira',
  codigo: 'FER-0012',
  descricao: 'Furadeira de impacto 700W',
  unidade: 'UN',
  unidadeSimbolo: 'un',
  tipo: 'FERRAMENTA',
  ativo: true,
  quantidadeMinima: 1,
  saldo: 2,
  status: 'NORMAL',
  ultimasMovimentacoes: [
    {
      id: 'm1',
      canteiroId: 'c-aurora',
      canteiroNome: 'Residencial Aurora',
      itemId: 'i-furadeira',
      itemCodigo: 'FER-0012',
      itemDescricao: 'Furadeira de impacto 700W',
      unidadeSimbolo: 'un',
      tipo: 'SAIDA',
      motivo: 'RETIRADA',
      quantidade: 1,
      saldoApos: 2,
      colaboradorId: 'col-1',
      colaboradorNome: 'João da Silva',
      frenteObra: 'Fundação',
      observacao: null,
      estornaId: null,
      estornadaPorId: null,
      registradoPorId: 'u-almoxarife',
      registradoPorNome: 'Sebastião Lima',
      criadoEm: '2026-11-01T12:00:00Z',
    },
  ],
};

function montar(usuario: UsuarioSessao) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <DetalheItemTela itemId="i-furadeira" />
    </ComSessao>,
  );
}

describe('DetalheItemTela (S07)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('mostra saldo contra o mínimo, tipo, unidade e as últimas movimentações', async () => {
    jest.mocked(buscarDetalheEstoque).mockResolvedValue(furadeira);
    await montar(almoxarife);

    expect(await screen.findByLabelText('Saldo')).toBeTruthy();
    expect(screen.getByText('2 un')).toBeTruthy();
    expect(screen.getByText('Mínimo: 1 un')).toBeTruthy();
    expect(screen.getByText('Ferramenta')).toBeTruthy();
    expect(screen.getByText('Saída −1 un')).toBeTruthy();
    expect(screen.getByText(/Retirada · João da Silva · Fundação/)).toBeTruthy();
    expect(buscarDetalheEstoque).toHaveBeenCalledWith('c-aurora', 'i-furadeira');
  });

  it('oferece Registrar saída ao ALMOXARIFE com o item já escolhido', async () => {
    jest.mocked(buscarDetalheEstoque).mockResolvedValue(furadeira);
    await montar(almoxarife);

    await fireEvent.press(await screen.findByText('Registrar saída'));
    expect(mockPush).toHaveBeenCalledWith('/movimentar/saida?itemId=i-furadeira');
    expect(screen.queryByText('Solicitar material')).toBeNull();
  });

  it('oferece Solicitar material ao GESTOR', async () => {
    jest.mocked(buscarDetalheEstoque).mockResolvedValue(furadeira);
    await montar(gestor);

    await fireEvent.press(await screen.findByText('Solicitar material'));
    expect(mockPush).toHaveBeenCalledWith('/requisicoes/nova?itemId=i-furadeira');
    expect(screen.queryByText('Registrar saída')).toBeNull();
  });

  it('desabilita a saída quando não há saldo', async () => {
    jest
      .mocked(buscarDetalheEstoque)
      .mockResolvedValue({ ...furadeira, saldo: 0, status: 'ZERADO', ultimasMovimentacoes: [] });
    await montar(almoxarife);

    expect(await screen.findByText('Sem movimentações')).toBeTruthy();
    expect(screen.getByText('Registrar saída')).toBeDisabled();
  });
});
