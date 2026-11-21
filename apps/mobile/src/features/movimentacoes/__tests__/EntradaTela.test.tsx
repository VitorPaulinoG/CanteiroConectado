import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { listarColaboradores } from '@/shared/colaborador/api';
import { listarEstoque } from '@/shared/estoque/api';
import { erroDaApi } from '@/test-utils/api';
import { furadeiraNoEstoque, joao, pagina, saidaGravada } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { registrarMovimentacao } from '../api';
import { EntradaTela } from '../telas/EntradaTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ registrarMovimentacao: jest.fn() }));
jest.mock('@/shared/estoque/api', () => ({
  ...jest.requireActual('@/shared/estoque/api'),
  listarEstoque: jest.fn(),
}));
jest.mock('@/shared/colaborador/api', () => ({
  ...jest.requireActual('@/shared/colaborador/api'),
  listarColaboradores: jest.fn(),
}));

const mockReplace = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: jest.fn(), replace: mockReplace }),
}));

const cimento = {
  ...furadeiraNoEstoque,
  itemId: 'i-cimento',
  codigo: 'MAT-0001',
  descricao: 'Cimento CP-II 50kg',
  unidade: 'SACO' as const,
  unidadeSimbolo: 'saco',
  tipo: 'MATERIAL' as const,
  quantidadeMinima: 50,
  saldo: 30,
  status: 'ABAIXO_DO_MINIMO' as const,
};

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <EntradaTela />
    </ComSessao>,
  );
}

async function escolher(descricao: string) {
  await fireEvent.press(screen.getByLabelText('Escolher item'));
  await fireEvent.press(await screen.findByText(descricao));
}

describe('EntradaTela (S10)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.mocked(listarEstoque).mockResolvedValue(pagina([cimento, furadeiraNoEstoque]));
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([joao]));
  });

  it('registra o recebimento de 50 sacos de cimento e mostra o comprovante salvo', async () => {
    jest.mocked(registrarMovimentacao).mockResolvedValue({
      ...saidaGravada,
      tipo: 'ENTRADA',
      motivo: 'RECEBIMENTO',
      itemDescricao: 'Cimento CP-II 50kg',
      unidadeSimbolo: 'saco',
      quantidade: 50,
      saldoApos: 80,
      colaboradorId: null,
      colaboradorNome: null,
      frenteObra: null,
      observacao: 'NF 778',
    });
    await montar();
    expect(screen.queryByLabelText('Quem está devolvendo')).toBeNull();

    await escolher('Cimento CP-II 50kg');
    await fireEvent.changeText(screen.getByLabelText('Quantidade'), '50');
    await fireEvent.changeText(screen.getByLabelText('Observação'), 'NF 778');
    await fireEvent.press(screen.getByText('Confirmar entrada'));

    expect(await screen.findByText('Salvo no servidor')).toBeTruthy();
    expect(screen.getByText('80 saco')).toBeTruthy();
    expect(registrarMovimentacao).toHaveBeenCalledWith(
      'c-aurora',
      expect.objectContaining({
        tipo: 'ENTRADA',
        motivo: 'RECEBIMENTO',
        itemId: 'i-cimento',
        quantidade: 50,
        colaboradorId: undefined,
        observacao: 'NF 778',
      }),
    );
    await fireEvent.press(screen.getByText('Nova entrada'));
    expect(mockReplace).toHaveBeenCalledWith('/movimentar/entrada');
  });

  it('mostra o colaborador só na devolução e exige ferramenta', async () => {
    await montar();
    await fireEvent.press(screen.getByText('Devolução'));
    expect(screen.getByLabelText('Quem está devolvendo')).toBeTruthy();

    await escolher('Cimento CP-II 50kg');
    await fireEvent.press(screen.getByText('Confirmar entrada'));

    expect(
      screen.getByText('Só ferramentas podem ser devolvidas. Para material, use Ajuste.'),
    ).toBeTruthy();
    expect(screen.getByText('Escolha quem está devolvendo.')).toBeTruthy();
    expect(registrarMovimentacao).not.toHaveBeenCalled();
  });

  it('registra a devolução da furadeira pelo João', async () => {
    jest.mocked(registrarMovimentacao).mockResolvedValue({
      ...saidaGravada,
      tipo: 'ENTRADA',
      motivo: 'DEVOLUCAO',
      saldoApos: 3,
    });
    await montar();
    await fireEvent.press(screen.getByText('Devolução'));
    await escolher('Furadeira de impacto 700W');
    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));
    await fireEvent.press(await screen.findByText('João da Silva'));
    await fireEvent.press(screen.getByText('Confirmar entrada'));

    await waitFor(() =>
      expect(registrarMovimentacao).toHaveBeenCalledWith(
        'c-aurora',
        expect.objectContaining({ motivo: 'DEVOLUCAO', colaboradorId: 'col-joao', quantidade: 1 }),
      ),
    );
  });

  it('volta ao formulário com a mensagem quando a API recusa a quantidade', async () => {
    jest.mocked(registrarMovimentacao).mockRejectedValue(
      erroDaApi(422, {
        codigo: 'QUANTIDADE_INVALIDA',
        detail: 'A quantidade aceita no máximo 3 casas decimais.',
        campo: 'quantidade',
      }),
    );
    await montar();
    await escolher('Cimento CP-II 50kg');
    await fireEvent.press(screen.getByText('Confirmar entrada'));

    expect(
      await screen.findByText('Informe uma quantidade maior que zero, com até 3 casas decimais.'),
    ).toBeTruthy();
    expect(screen.getByText('Confirmar entrada')).toBeTruthy();
  });
});
