import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { buscarItem, type Item } from '@/shared/item/api';
import { erroDaApi } from '@/test-utils/api';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { atualizarItem, criarItem } from '../itens';
import { ItemFormTela } from '../telas/ItemFormTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../itens', () => ({ criarItem: jest.fn(), atualizarItem: jest.fn() }));
jest.mock('@/shared/item/api', () => ({
  ...jest.requireActual('@/shared/item/api'),
  buscarItem: jest.fn(),
}));

const mockBack = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: jest.fn(), back: mockBack }) }));

const cimento: Item = {
  id: 'i-cimento',
  codigo: 'MAT-0001',
  descricao: 'Cimento CP-II 50kg',
  unidade: 'SACO',
  unidadeSimbolo: 'saco',
  tipo: 'MATERIAL',
  quantidadeMinima: 50,
  ativo: true,
};

function montar(id?: string) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <ItemFormTela id={id} />
    </ComSessao>,
  );
}

async function preencherFuradeira() {
  await fireEvent.changeText(screen.getByLabelText('Código'), ' FER-0012 ');
  await fireEvent.changeText(screen.getByLabelText('Descrição'), 'Furadeira de impacto 700W');
  await fireEvent.press(screen.getByLabelText('Escolher unidade'));
  await fireEvent.press(await screen.findByText('un'));
  await fireEvent.press(screen.getByText('Ferramenta'));
  await fireEvent.changeText(screen.getByLabelText('Quantidade mínima'), '1');
}

describe('ItemFormTela (S21)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('valida os campos obrigatórios e o mínimo', async () => {
    await montar();
    await fireEvent.changeText(screen.getByLabelText('Quantidade mínima'), '-2');
    await fireEvent.press(screen.getByText('Salvar'));

    expect(screen.getByText('Informe o código.')).toBeTruthy();
    expect(screen.getByText('Informe a descrição.')).toBeTruthy();
    expect(screen.getByText('Escolha a unidade.')).toBeTruthy();
    expect(screen.getByText('Escolha o tipo.')).toBeTruthy();
    expect(
      screen.getByText('Informe um número igual ou maior que zero, com até 3 casas decimais.'),
    ).toBeTruthy();
    expect(criarItem).not.toHaveBeenCalled();
  });

  it('cadastra a furadeira do cenário e volta para a lista', async () => {
    jest.mocked(criarItem).mockResolvedValue({ ...cimento, id: 'novo' });
    await montar();
    await preencherFuradeira();
    await fireEvent.press(screen.getByText('Salvar'));

    await waitFor(() => expect(mockBack).toHaveBeenCalled());
    expect(criarItem).toHaveBeenCalledWith({
      codigo: 'FER-0012',
      descricao: 'Furadeira de impacto 700W',
      unidade: 'UN',
      tipo: 'FERRAMENTA',
      quantidadeMinima: 1,
    });
  });

  it('mostra o código duplicado no próprio campo', async () => {
    jest
      .mocked(criarItem)
      .mockRejectedValue(
        erroDaApi(409, {
          codigo: 'CODIGO_DUPLICADO',
          detail: 'Código já cadastrado.',
          campo: 'codigo',
        }),
      );
    await montar();
    await preencherFuradeira();
    await fireEvent.press(screen.getByText('Salvar'));

    expect(await screen.findByText('Código já cadastrado.')).toBeTruthy();
    expect(mockBack).not.toHaveBeenCalled();
  });

  it('mostra a explicação da API quando o item já teve movimentação', async () => {
    jest.mocked(buscarItem).mockResolvedValue(cimento);
    jest.mocked(atualizarItem).mockRejectedValue(
      erroDaApi(422, {
        codigo: 'ITEM_JA_MOVIMENTADO',
        detail: 'Este item já teve movimentação: o código e a unidade não podem mais mudar.',
        campo: 'codigo',
      }),
    );
    await montar('i-cimento');
    await fireEvent.changeText(await screen.findByLabelText('Código'), 'MAT-0002');
    await fireEvent.press(screen.getByText('Salvar'));

    expect(
      await screen.findByText(
        'Este item já teve movimentação: o código e a unidade não podem mais mudar.',
      ),
    ).toBeTruthy();
    expect(atualizarItem).toHaveBeenCalledWith('i-cimento', {
      codigo: 'MAT-0002',
      descricao: 'Cimento CP-II 50kg',
      tipo: 'MATERIAL',
      quantidadeMinima: 50,
      ativo: true,
    });
  });

  it('pede confirmação ao desativar', async () => {
    jest.mocked(buscarItem).mockResolvedValue(cimento);
    jest.mocked(atualizarItem).mockResolvedValue({ ...cimento, ativo: false });
    await montar('i-cimento');
    await screen.findByDisplayValue('Cimento CP-II 50kg');

    await fireEvent(screen.getByLabelText('Ativo'), 'valueChange', false);
    await fireEvent.press(screen.getByText('Salvar'));
    expect(screen.getByText('Desativar o item?')).toBeTruthy();
    expect(atualizarItem).not.toHaveBeenCalled();

    await fireEvent.press(screen.getByText('Desativar'));
    await waitFor(() =>
      expect(atualizarItem).toHaveBeenCalledWith(
        'i-cimento',
        expect.objectContaining({ ativo: false }),
      ),
    );
  });
});
