import { screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { requisicaoDaCamila } from '@/test-utils/requisicoes';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { buscarRequisicao, type Requisicao } from '../api';
import { DetalheRequisicaoTela } from '../telas/DetalheRequisicaoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), buscarRequisicao: jest.fn() }));
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: jest.fn() }),
  useFocusEffect: jest.fn(),
}));

function montar(requisicao: Requisicao) {
  jest.mocked(buscarRequisicao).mockResolvedValue(requisicao);
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <DetalheRequisicaoTela id={requisicao.id} />
    </ComSessao>,
  );
}

describe('DetalheRequisicaoTela (S14)', () => {
  it('mostra itens, quantidades, prazo, frente, observação e a linha do tempo da pendente', async () => {
    await montar(requisicaoDaCamila);

    expect(await screen.findByText('Cimento CP-II 50kg')).toBeTruthy();
    expect(screen.getByText('40 saco')).toBeTruthy();
    expect(screen.getByText('10/11/2026')).toBeTruthy();
    expect(screen.getByText('Laje do 3º andar')).toBeTruthy();
    expect(screen.getByText('Urgente')).toBeTruthy();
    expect(screen.getByText('Criada por Camila Souza')).toBeTruthy();
    expect(screen.getByText('Pendente')).toBeTruthy();
    expect(screen.queryByLabelText('Previsão de chegada')).toBeNull();
  });

  it('destaca a previsão de chegada e quem decidiu na aprovada', async () => {
    await montar({
      ...requisicaoDaCamila,
      status: 'APROVADA',
      previsaoChegada: '2026-11-15',
      decididoPorId: 'u-almoxarife',
      decididoPorNome: 'Sebastião Lima',
      decididoEm: '2026-11-02T12:00:00Z',
      observacaoDecisao: 'Pedido feito ao fornecedor',
      historico: [
        ...requisicaoDaCamila.historico,
        {
          status: 'APROVADA',
          usuarioId: 'u-almoxarife',
          usuarioNome: 'Sebastião Lima',
          observacao: 'Pedido feito ao fornecedor',
          criadoEm: '2026-11-02T12:00:00Z',
        },
      ],
    });

    expect(await screen.findByLabelText('Previsão de chegada')).toBeTruthy();
    expect(screen.getByText('15/11/2026')).toBeTruthy();
    expect(screen.getByText('Decidida por Sebastião Lima')).toBeTruthy();
    expect(screen.getByText('Aprovada por Sebastião Lima')).toBeTruthy();
  });

  it('mostra o motivo da recusa', async () => {
    await montar({
      ...requisicaoDaCamila,
      status: 'RECUSADA',
      decididoPorNome: 'Sebastião Lima',
      observacaoDecisao: 'Material já está no canteiro',
    });

    expect(await screen.findByLabelText('Motivo da recusa')).toBeTruthy();
    expect(screen.getByText('Material já está no canteiro')).toBeTruthy();
    expect(screen.getByText('Recusada')).toBeTruthy();
  });

  it.each([
    ['ATENDIDA', 'Atendida'],
    ['CANCELADA', 'Cancelada'],
  ] as const)('mostra o selo %s', async (status, rotulo) => {
    await montar({ ...requisicaoDaCamila, status });
    expect(await screen.findByText(rotulo)).toBeTruthy();
  });
});
