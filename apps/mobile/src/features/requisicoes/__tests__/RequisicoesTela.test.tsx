import { fireEvent, screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { requisicaoDaCamila, resumoDe } from '@/test-utils/requisicoes';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { listarRequisicoes } from '../api';
import { RequisicoesTela } from '../telas/RequisicoesTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), listarRequisicoes: jest.fn() }));

const mockPush = jest.fn();
let mockParametros: { aba?: string } = {};
const mockFocos: (() => void)[] = [];
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush, back: jest.fn() }),
  useLocalSearchParams: () => mockParametros,
  useFocusEffect: (callback: () => void) => mockFocos.push(callback),
}));

function pagina(conteudo: ReturnType<typeof resumoDe>[]) {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <RequisicoesTela />
    </ComSessao>,
  );
}

describe('RequisicoesTela (S12)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockParametros = {};
  });

  it('mostra Minhas com número, itens resumidos, prazo e selo, e abre o detalhe', async () => {
    jest.mocked(listarRequisicoes).mockResolvedValue(
      pagina([
        {
          ...resumoDe(requisicaoDaCamila),
          itens: [
            ...requisicaoDaCamila.itens,
            {
              itemId: 'i2',
              codigo: 'MAT-2',
              descricao: 'Areia',
              unidadeSimbolo: 'm³',
              quantidade: 2,
            },
          ],
        },
      ]),
    );
    await montar();

    expect(await screen.findByText('REQ-0001')).toBeTruthy();
    expect(screen.getByText('Cimento CP-II 50kg (40 saco) e mais 1')).toBeTruthy();
    expect(screen.getByText('Necessário até 10/11/2026')).toBeTruthy();
    expect(screen.getByText('Pendente')).toBeTruthy();
    expect(listarRequisicoes).toHaveBeenCalledWith('c-aurora', 'minhas', 0);

    await fireEvent.press(screen.getByLabelText('REQ-0001'));
    expect(mockPush).toHaveBeenCalledWith('/requisicoes/r-1');
  });

  it('troca para Para decidir e mostra quem pediu', async () => {
    jest
      .mocked(listarRequisicoes)
      .mockResolvedValueOnce(pagina([]))
      .mockResolvedValue(pagina([resumoDe(requisicaoDaCamila)]));
    await montar();
    expect(await screen.findByText('Nenhuma requisição sua')).toBeTruthy();

    await fireEvent.press(screen.getByText('Para decidir'));

    expect(await screen.findByText('Pedida por Camila Souza')).toBeTruthy();
    expect(listarRequisicoes).toHaveBeenLastCalledWith('c-aurora', 'para-decidir', 0);
  });

  it('abre na aba Para decidir quando vem do Início', async () => {
    mockParametros = { aba: 'para-decidir' };
    jest.mocked(listarRequisicoes).mockResolvedValue(pagina([]));
    await montar();

    expect(await screen.findByText('Nada para decidir')).toBeTruthy();
    expect(listarRequisicoes).toHaveBeenCalledWith('c-aurora', 'para-decidir', 0);
  });

  it('atualiza ao voltar para a tela e abre a nova requisição', async () => {
    jest.mocked(listarRequisicoes).mockResolvedValue(pagina([resumoDe(requisicaoDaCamila)]));
    await montar();
    await screen.findByText('REQ-0001');
    const aoFocar = mockFocos[mockFocos.length - 1];
    aoFocar();
    aoFocar();

    await screen.findByText('REQ-0001');
    expect(jest.mocked(listarRequisicoes).mock.calls.length).toBeGreaterThanOrEqual(2);
    await fireEvent.press(screen.getByText('Nova requisição'));
    expect(mockPush).toHaveBeenCalledWith('/requisicoes/nova');
  });
});
