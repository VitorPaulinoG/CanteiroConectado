import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { erroDaApi } from '@/test-utils/api';
import { escolherNoCalendario } from '@/test-utils/data';
import { renderizar } from '@/test-utils/render';
import { requisicaoDaCamila } from '@/test-utils/requisicoes';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { aprovarRequisicao, buscarRequisicao, recusarRequisicao } from '../api';
import { DetalheRequisicaoTela } from '../telas/DetalheRequisicaoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({
  ...jest.requireActual('../api'),
  buscarRequisicao: jest.fn(),
  aprovarRequisicao: jest.fn(),
  recusarRequisicao: jest.fn(),
}));
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: jest.fn() }),
  useFocusEffect: jest.fn(),
}));

const paraDecidir = {
  ...requisicaoDaCamila,
  acoes: { decidir: true, atender: false, cancelar: false },
};

function daquiA(dias: number) {
  const data = new Date();
  data.setDate(data.getDate() + dias);
  data.setHours(0, 0, 0, 0);
  return data;
}

function iso(data: Date) {
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
}

async function abrir(requisicao = paraDecidir) {
  jest.mocked(buscarRequisicao).mockResolvedValue(requisicao);
  await renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <DetalheRequisicaoTela id={requisicao.id} />
    </ComSessao>,
  );
}

describe('Decisão em S14 e S15', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    escolherNoCalendario(null);
  });

  it('não mostra os botões de decisão para o autor', async () => {
    await abrir(requisicaoDaCamila);
    await screen.findByText('Cimento CP-II 50kg');
    expect(screen.queryByText('Aprovar ou recusar')).toBeNull();
  });

  it('aprova informando a previsão de chegada', async () => {
    const aprovada = {
      ...paraDecidir,
      status: 'APROVADA' as const,
      previsaoChegada: iso(daquiA(14)),
      acoes: { decidir: false, atender: true, cancelar: false },
    };
    jest.mocked(aprovarRequisicao).mockResolvedValue(aprovada);
    await abrir();
    await fireEvent.press(await screen.findByText('Aprovar ou recusar'));
    jest.mocked(buscarRequisicao).mockResolvedValue(aprovada);

    escolherNoCalendario(daquiA(14));
    await fireEvent.press(screen.getByLabelText('Escolher previsão de chegada'));
    await fireEvent.changeText(screen.getByLabelText('Recado para quem pediu'), ' Pedido feito ');
    await fireEvent.press(screen.getByText('Confirmar aprovação'));

    await waitFor(() =>
      expect(aprovarRequisicao).toHaveBeenCalledWith('r-1', {
        previsaoChegada: iso(daquiA(14)),
        observacao: 'Pedido feito',
      }),
    );
    expect(await screen.findByLabelText('Previsão de chegada')).toBeTruthy();
    expect(screen.queryByText('Aprovar ou recusar')).toBeNull();
  });

  it('exige o motivo na recusa', async () => {
    jest.mocked(recusarRequisicao).mockResolvedValue({
      ...paraDecidir,
      status: 'RECUSADA',
      observacaoDecisao: 'Material já está no canteiro',
      acoes: { decidir: false, atender: false, cancelar: false },
    });
    await abrir();
    await fireEvent.press(await screen.findByText('Aprovar ou recusar'));
    await fireEvent.press(screen.getByText('Recusar'));

    await fireEvent.press(screen.getByText('Confirmar recusa'));
    expect(screen.getByText('Conte o motivo da recusa (pelo menos 5 caracteres).')).toBeTruthy();
    expect(recusarRequisicao).not.toHaveBeenCalled();

    await fireEvent.changeText(
      screen.getByLabelText('Motivo da recusa'),
      'Material já está no canteiro',
    );
    await fireEvent.press(screen.getByText('Confirmar recusa'));
    await waitFor(() =>
      expect(recusarRequisicao).toHaveBeenCalledWith('r-1', 'Material já está no canteiro'),
    );
  });

  it('mostra a mensagem da API e recarrega quando a requisição já foi decidida', async () => {
    jest.mocked(aprovarRequisicao).mockRejectedValue(
      erroDaApi(409, {
        codigo: 'REQUISICAO_NAO_PENDENTE',
        detail: 'Esta requisição já foi aprovada e não pode mais ser decidida.',
        status: 'APROVADA',
      }),
    );
    await abrir();
    await fireEvent.press(await screen.findByText('Aprovar ou recusar'));
    await fireEvent.press(screen.getByText('Confirmar aprovação'));

    expect(
      await screen.findByText('Esta requisição já foi aprovada e não pode mais ser decidida.'),
    ).toBeTruthy();
    await waitFor(() => expect(buscarRequisicao).toHaveBeenCalledTimes(2));
  });
});
