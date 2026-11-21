import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';

import { buscarDetalheEstoque, listarEstoque } from '@/shared/estoque/api';
import { erroDaApi, erroDeRede } from '@/test-utils/api';
import { escolherNoCalendario } from '@/test-utils/data';
import { cimentoNoEstoque, furadeiraNoEstoque, pagina } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';
import { requisicaoDaCamila } from '@/test-utils/requisicoes';
import { autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { criarRequisicao } from '../api';
import { NovaRequisicaoTela } from '../telas/NovaRequisicaoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), criarRequisicao: jest.fn() }));
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  listarEstoque: jest.fn(),
  buscarDetalheEstoque: jest.fn(),
}));

const mockBack = jest.fn();
const mockReplace = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: mockBack, replace: mockReplace }),
}));

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

function daquiA(dias: number) {
  const data = new Date();
  data.setDate(data.getDate() + dias);
  data.setHours(0, 0, 0, 0);
  return data;
}

function iso(data: Date) {
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
}

function montar(itemIdInicial?: string) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(gestor))}>
      <NovaRequisicaoTela itemIdInicial={itemIdInicial} />
    </ComSessao>,
  );
}

async function adicionar(descricao: string) {
  await fireEvent.press(screen.getByText('Adicionar item'));
  const lista = await screen.findByTestId('lista-do-seletor-de-item');
  await fireEvent.press(await within(lista).findByText(descricao));
}

async function escolherData(dias: number) {
  escolherNoCalendario(daquiA(dias));
  await fireEvent.press(screen.getByLabelText('Escolher necessário até'));
}

describe('NovaRequisicaoTela (S13)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    escolherNoCalendario(null);
    jest.mocked(listarEstoque).mockResolvedValue(pagina([cimentoNoEstoque, furadeiraNoEstoque]));
  });

  it('cria a requisição de 40 sacos de cimento vinda do Solicitar material de S07', async () => {
    jest
      .mocked(buscarDetalheEstoque)
      .mockResolvedValue({ ...cimentoNoEstoque, ativo: true, ultimasMovimentacoes: [] });
    jest.mocked(criarRequisicao).mockResolvedValue(requisicaoDaCamila);
    await montar('i-cimento');

    expect(await screen.findByText('Cimento CP-II 50kg')).toBeTruthy();
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(5);
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), ' Laje do 3º andar ');
    await fireEvent.press(screen.getByText('Enviar requisição'));

    expect(await screen.findByText('Requisição REQ-0001')).toBeTruthy();
    expect(screen.getByText('Salvo no servidor')).toBeTruthy();
    await fireEvent.press(screen.getByText('Ver requisição'));
    expect(mockReplace).toHaveBeenCalledWith('/requisicoes/r-1');
    expect(criarRequisicao).toHaveBeenCalledWith({
      id: expect.stringMatching(UUID),
      canteiroId: 'c-aurora',
      necessarioAte: iso(daquiA(5)),
      frenteObra: 'Laje do 3º andar',
      observacao: undefined,
      itens: [{ itemId: 'i-cimento', quantidade: 40 }],
    });
  });

  it('adiciona e remove itens e não repete o mesmo item', async () => {
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await adicionar('Furadeira de impacto 700W');
    await adicionar('Cimento CP-II 50kg');

    expect(
      screen.getByText('Cimento CP-II 50kg já está na lista. Ajuste a quantidade na linha dele.'),
    ).toBeTruthy();
    expect(screen.getAllByText(/Saldo/)).toHaveLength(2);

    await fireEvent.press(screen.getByLabelText('Remover Furadeira de impacto 700W'));
    expect(screen.queryByLabelText('Quantidade de Furadeira de impacto 700W')).toBeNull();
  });

  it('valida itens, quantidade e data antes de enviar', async () => {
    await montar();
    await fireEvent.press(screen.getByText('Enviar requisição'));
    expect(screen.getByText('Adicione pelo menos um item.')).toBeTruthy();
    expect(screen.getByText('Informe até quando o material é necessário.')).toBeTruthy();

    await adicionar('Cimento CP-II 50kg');
    await fireEvent.press(screen.getByText('Enviar requisição'));
    expect(screen.getByText('Informe uma quantidade maior que zero.')).toBeTruthy();
    expect(criarRequisicao).not.toHaveBeenCalled();
  });

  it('mostra no campo a recusa da data pela API', async () => {
    jest
      .mocked(criarRequisicao)
      .mockRejectedValue(erroDaApi(422, { codigo: 'DATA_NO_PASSADO', campo: 'necessarioAte' }));
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(0);
    await fireEvent.press(screen.getByText('Enviar requisição'));

    expect(await screen.findByText('A data não pode ser anterior a hoje.')).toBeTruthy();
  });

  it('mostra na linha o item recusado pela API', async () => {
    jest
      .mocked(criarRequisicao)
      .mockRejectedValue(erroDaApi(422, { codigo: 'ITEM_INATIVO', campo: 'itens[0].itemId' }));
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(2);
    await fireEvent.press(screen.getByText('Enviar requisição'));

    expect(await screen.findByText('Este item está desativado.')).toBeTruthy();
  });

  it('reenvia com o mesmo id quando não houve resposta', async () => {
    jest
      .mocked(criarRequisicao)
      .mockRejectedValueOnce(erroDeRede())
      .mockResolvedValue(requisicaoDaCamila);
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(2);
    await fireEvent.press(screen.getByText('Enviar requisição'));

    await fireEvent.press(await screen.findByText('Tentar de novo'));

    expect(await screen.findByText('Salvo no servidor')).toBeTruthy();
    const [primeira, segunda] = jest.mocked(criarRequisicao).mock.calls;
    expect(segunda[0].id).toBe(primeira[0].id);
    await fireEvent.press(screen.getByText('Concluir'));
    await waitFor(() => expect(mockBack).toHaveBeenCalled());
  });

  it('envia frente e observação sem os espaços das pontas', async () => {
    jest.mocked(criarRequisicao).mockResolvedValue(requisicaoDaCamila);
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(3);
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), '  Laje  ');
    await fireEvent.changeText(screen.getByLabelText('Observação'), ' Entregar pela manhã ');
    await fireEvent.press(screen.getByText('Enviar requisição'));

    await screen.findByText('Salvo no servidor');
    expect(criarRequisicao).toHaveBeenCalledWith(
      expect.objectContaining({ frenteObra: 'Laje', observacao: 'Entregar pela manhã' }),
    );
  });

  it('mostra acima do botão a recusa da API que não é de um campo', async () => {
    jest
      .mocked(criarRequisicao)
      .mockRejectedValue(erroDaApi(422, { codigo: 'CANTEIRO_INATIVO', detail: 'Inativo.' }));
    await montar();
    await adicionar('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade de Cimento CP-II 50kg'), '40');
    await escolherData(2);
    await fireEvent.press(screen.getByText('Enviar requisição'));

    expect(await screen.findByText('Este canteiro está desativado.')).toBeTruthy();
    expect(screen.getByText('Enviar requisição')).toBeTruthy();
  });

  it('fecha o seletor de item sem adicionar nada', async () => {
    await montar();
    await fireEvent.press(screen.getByText('Adicionar item'));
    await screen.findByTestId('lista-do-seletor-de-item');

    await fireEvent.press(screen.getByLabelText('Fechar'));

    await waitFor(() => expect(screen.queryByTestId('lista-do-seletor-de-item')).toBeNull());
    await fireEvent.press(screen.getByText('Enviar requisição'));
    expect(screen.getByText('Adicione pelo menos um item.')).toBeTruthy();
  });
});
