import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { listarColaboradores } from '@/shared/colaborador/api';
import { buscarDetalheEstoque, listarEstoque } from '@/shared/estoque/api';
import { erroDaApi, erroDeRede } from '@/test-utils/api';
import { furadeiraNoEstoque, joao, pagina } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { registrarMovimentacao } from '../api';
import { SaidaTela } from '../telas/SaidaTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ registrarMovimentacao: jest.fn() }));
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  listarEstoque: jest.fn(),
  buscarDetalheEstoque: jest.fn(),
}));
jest.mock('@/shared/colaborador/api', () => ({
  ...jest.requireActual('@/shared/colaborador/api'),
  listarColaboradores: jest.fn(),
}));

const mockBack = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: jest.fn(), back: mockBack }) }));

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

function montar(usuario: UsuarioSessao = almoxarife, itemIdInicial?: string) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <SaidaTela itemIdInicial={itemIdInicial} />
    </ComSessao>,
  );
}

async function escolherFuradeiraEJoao() {
  await fireEvent.press(screen.getByLabelText('Escolher item'));
  await fireEvent.press(await screen.findByText('Furadeira de impacto 700W'));
  await fireEvent.press(screen.getByLabelText('Escolher colaborador'));
  await fireEvent.press(await screen.findByText('João da Silva'));
}

describe('SaidaTela (S09)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.mocked(listarEstoque).mockResolvedValue(pagina([furadeiraNoEstoque]));
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([joao]));
  });

  it('registra a retirada do João na Fundação com o id gerado ao abrir', async () => {
    jest.mocked(registrarMovimentacao).mockResolvedValue({} as never);
    await montar();

    await escolherFuradeiraEJoao();
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), ' Fundação ');
    await fireEvent.press(screen.getByText('Confirmar saída'));

    await waitFor(() => expect(mockBack).toHaveBeenCalled());
    expect(registrarMovimentacao).toHaveBeenCalledWith('c-aurora', {
      id: expect.stringMatching(UUID),
      tipo: 'SAIDA',
      motivo: 'RETIRADA',
      itemId: 'i-furadeira',
      quantidade: 1,
      colaboradorId: 'col-joao',
      frenteObra: 'Fundação',
      observacao: undefined,
    });
    expect(screen.getByText('Saldo no canteiro: 2 un')).toBeTruthy();
  });

  it('impede no campo a quantidade acima do saldo', async () => {
    await montar();
    await escolherFuradeiraEJoao();
    await fireEvent.changeText(screen.getByLabelText('Quantidade'), '3');
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), 'Fundação');
    await fireEvent.press(screen.getByText('Confirmar saída'));

    expect(screen.getByText('A quantidade passa do saldo (2 un).')).toBeTruthy();
    expect(registrarMovimentacao).not.toHaveBeenCalled();
  });

  it('mostra SALDO_INSUFICIENTE da API no campo de quantidade', async () => {
    jest.mocked(registrarMovimentacao).mockRejectedValue(
      erroDaApi(422, {
        codigo: 'SALDO_INSUFICIENTE',
        detail: 'Saldo insuficiente: há 1 no canteiro.',
        campo: 'quantidade',
        saldoAtual: 1,
      }),
    );
    await montar();
    await escolherFuradeiraEJoao();
    await fireEvent.changeText(screen.getByLabelText('Quantidade'), '2');
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), 'Fundação');
    await fireEvent.press(screen.getByText('Confirmar saída'));

    expect(await screen.findByText('Saldo insuficiente: há 1 no canteiro.')).toBeTruthy();
    expect(mockBack).not.toHaveBeenCalled();
  });

  it('no ajuste dispensa o colaborador e exige a observação', async () => {
    jest.mocked(registrarMovimentacao).mockResolvedValue({} as never);
    await montar();
    await fireEvent.press(screen.getByLabelText('Escolher item'));
    await fireEvent.press(await screen.findByText('Furadeira de impacto 700W'));
    await fireEvent.press(screen.getByText('Ajuste (perda, dano)'));
    await fireEvent.press(screen.getByText('Confirmar saída'));
    expect(
      screen.getByText('No ajuste, conte o que aconteceu (perda, furto, dano...).'),
    ).toBeTruthy();

    await fireEvent.changeText(screen.getByLabelText('Observação'), 'Quebrou no uso');
    await fireEvent.press(screen.getByText('Confirmar saída'));
    await waitFor(() =>
      expect(registrarMovimentacao).toHaveBeenCalledWith(
        'c-aurora',
        expect.objectContaining({
          motivo: 'AJUSTE',
          observacao: 'Quebrou no uso',
          colaboradorId: undefined,
        }),
      ),
    );
  });

  it('já vem com o item quando aberta pelo detalhe do item', async () => {
    jest
      .mocked(buscarDetalheEstoque)
      .mockResolvedValue({ ...furadeiraNoEstoque, ativo: true, ultimasMovimentacoes: [] });
    await montar(almoxarife, 'i-furadeira');

    expect(await screen.findByDisplayValue('Furadeira de impacto 700W (FER-0012)')).toBeTruthy();
    expect(buscarDetalheEstoque).toHaveBeenCalledWith('c-aurora', 'i-furadeira');
  });

  it('desabilita o botão enquanto envia e envia uma vez só no toque duplo', async () => {
    jest.mocked(registrarMovimentacao).mockReturnValue(new Promise(() => undefined));
    await montar();
    await escolherFuradeiraEJoao();
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), 'Fundação');

    await fireEvent.press(screen.getByText('Confirmar saída'));
    await fireEvent.press(screen.getByText('Enviando…'));

    expect(screen.getByText('Enviando…')).toBeDisabled();
    expect(registrarMovimentacao).toHaveBeenCalledTimes(1);
  });

  it('oferece Tentar de novo com o mesmo id quando não houve resposta', async () => {
    jest
      .mocked(registrarMovimentacao)
      .mockRejectedValueOnce(erroDeRede())
      .mockResolvedValue({} as never);
    await montar();
    await escolherFuradeiraEJoao();
    await fireEvent.changeText(screen.getByLabelText('Frente de obra'), 'Fundação');
    await fireEvent.press(screen.getByText('Confirmar saída'));

    await fireEvent.press(await screen.findByText('Tentar de novo'));

    await waitFor(() => expect(mockBack).toHaveBeenCalled());
    const [primeira, segunda] = jest.mocked(registrarMovimentacao).mock.calls;
    expect(segunda[1].id).toBe(primeira[1].id);
  });

  it('mostra acesso negado para o GESTOR', async () => {
    await montar(gestor);
    expect(screen.getByText('Você não tem acesso a esta área')).toBeTruthy();
  });
});
