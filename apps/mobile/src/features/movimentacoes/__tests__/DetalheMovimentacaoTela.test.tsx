import { fireEvent, screen } from '@testing-library/react-native';

import { chavesEstoque } from '@/shared/estoque';
import { erroDaApi, erroDeRede } from '@/test-utils/api';
import { saidaGravada } from '@/test-utils/estoque';
import { criarQueryClientDeTeste, renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { buscarMovimentacao, estornarMovimentacao } from '../api';
import { DetalheMovimentacaoTela, podeEstornar } from '../telas/DetalheMovimentacaoTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({
  ...jest.requireActual('../api'),
  buscarMovimentacao: jest.fn(),
  estornarMovimentacao: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush, back: jest.fn() }) }));

const estorno = {
  ...saidaGravada,
  id: 'm-estorno',
  tipo: 'ENTRADA' as const,
  motivo: 'ESTORNO' as const,
  saldoApos: 2,
  observacao: 'Lançada no item errado',
  estornaId: 'm-saida',
};

function montar(usuario: UsuarioSessao, queryClient = criarQueryClientDeTeste()) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <DetalheMovimentacaoTela id="m-saida" />
    </ComSessao>,
    queryClient,
  );
}

describe('DetalheMovimentacaoTela (S17)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('mostra tipo, motivo, item, quantidade, saldo, canteiro, frente, colaborador, autor e data', async () => {
    jest.mocked(buscarMovimentacao).mockResolvedValue(saidaGravada);
    await montar(gestor);

    expect(await screen.findByText('Saída · Retirada')).toBeTruthy();
    expect(screen.getByText('Furadeira de impacto 700W (FER-0012)')).toBeTruthy();
    expect(screen.getByText('−1 un')).toBeTruthy();
    expect(screen.getByText('1 un')).toBeTruthy();
    expect(screen.getByText('Residencial Aurora')).toBeTruthy();
    expect(screen.getByText('Fundação')).toBeTruthy();
    expect(screen.getByText('João da Silva')).toBeTruthy();
    expect(screen.getByText('Sebastião Lima')).toBeTruthy();
    expect(screen.queryByText('Estornar')).toBeNull();
  });

  it('estorna pelo ALMOXARIFE com motivo e atualiza estoque e histórico', async () => {
    jest.mocked(buscarMovimentacao).mockResolvedValue(saidaGravada);
    jest.mocked(estornarMovimentacao).mockResolvedValue(estorno);
    const queryClient = criarQueryClientDeTeste();
    queryClient.setQueryData(chavesEstoque.lista('c-aurora', {}), { pages: [] });
    queryClient.setQueryData(['movimentacoes', 'historico', 'c-aurora', {}], { pages: [] });
    await montar(almoxarife, queryClient);

    await fireEvent.press(await screen.findByText('Estornar'));
    expect(screen.getByText(/Será registrada uma entrada de 1 un de Furadeira/)).toBeTruthy();
    const botoes = screen.getAllByText('Estornar');
    await fireEvent.press(botoes[botoes.length - 1]);
    expect(screen.getByText('Conte por que o lançamento está errado.')).toBeTruthy();
    expect(estornarMovimentacao).not.toHaveBeenCalled();

    await fireEvent.changeText(
      screen.getByLabelText('Motivo do estorno'),
      ' Lançada no item errado ',
    );
    jest
      .mocked(buscarMovimentacao)
      .mockResolvedValue({ ...saidaGravada, estornadaPorId: 'm-estorno' });
    await fireEvent.press(screen.getAllByText('Estornar').slice(-1)[0]);

    expect(await screen.findByText('Estorno registrado')).toBeTruthy();
    expect(estornarMovimentacao).toHaveBeenCalledWith('m-saida', {
      id: expect.stringMatching(/^[0-9a-f-]{36}$/),
      motivo: 'Lançada no item errado',
    });
    expect(queryClient.getQueryState(chavesEstoque.lista('c-aurora', {}))?.isInvalidated).toBe(
      true,
    );
    expect(
      queryClient.getQueryState(['movimentacoes', 'historico', 'c-aurora', {}])?.isInvalidated,
    ).toBe(true);

    await fireEvent.press(screen.getByText('Fechar'));
    expect(await screen.findByText('Estornada')).toBeTruthy();
    expect(screen.queryByText('Estornar')).toBeNull();
  });

  it('reenvia o estorno com o mesmo id quando não houve resposta', async () => {
    jest.mocked(buscarMovimentacao).mockResolvedValue(saidaGravada);
    jest
      .mocked(estornarMovimentacao)
      .mockRejectedValueOnce(erroDeRede())
      .mockResolvedValue(estorno);
    await montar(almoxarife);

    await fireEvent.press(await screen.findByText('Estornar'));
    await fireEvent.changeText(screen.getByLabelText('Motivo do estorno'), 'Engano');
    await fireEvent.press(screen.getAllByText('Estornar').slice(-1)[0]);
    await fireEvent.press(await screen.findByText('Tentar de novo'));

    expect(await screen.findByText('Estorno registrado')).toBeTruthy();
    const [primeira, segunda] = jest.mocked(estornarMovimentacao).mock.calls;
    expect(segunda[1].id).toBe(primeira[1].id);
  });

  it('mostra a recusa da API no diálogo', async () => {
    jest.mocked(buscarMovimentacao).mockResolvedValue(saidaGravada);
    jest.mocked(estornarMovimentacao).mockRejectedValue(erroDaApi(409, { codigo: 'JA_ESTORNADA' }));
    await montar(almoxarife);

    await fireEvent.press(await screen.findByText('Estornar'));
    await fireEvent.changeText(screen.getByLabelText('Motivo do estorno'), 'Engano');
    await fireEvent.press(screen.getAllByText('Estornar').slice(-1)[0]);

    expect(await screen.findByText('Esta movimentação já foi estornada.')).toBeTruthy();
  });

  it('mostra o vínculo do estorno com a original e navega até ela', async () => {
    jest.mocked(buscarMovimentacao).mockResolvedValue(estorno);
    await montar(almoxarife);

    expect(await screen.findByText('Entrada · Estorno')).toBeTruthy();
    expect(screen.getByText('Motivo do estorno')).toBeTruthy();
    expect(screen.queryByText('Estornar')).toBeNull();
    await fireEvent.press(screen.getByText('Lançamento corrigido por este estorno'));
    expect(mockPush).toHaveBeenCalledWith('/movimentacao/m-saida');
  });
});

describe('podeEstornar', () => {
  it('só libera o ALMOXARIFE do canteiro, fora de estornos e de lançamentos já estornados', () => {
    const doAlmoxarife = { perfil: 'ALMOXARIFE', canteiroId: 'c-aurora' };
    expect(podeEstornar(saidaGravada, doAlmoxarife)).toBe(true);
    expect(podeEstornar(saidaGravada, { perfil: 'GESTOR', canteiroId: null })).toBe(false);
    expect(podeEstornar(saidaGravada, { perfil: 'ALMOXARIFE', canteiroId: 'c-outro' })).toBe(false);
    expect(podeEstornar(estorno, doAlmoxarife)).toBe(false);
    expect(podeEstornar({ ...saidaGravada, estornadaPorId: 'x' }, doAlmoxarife)).toBe(false);
  });
});
