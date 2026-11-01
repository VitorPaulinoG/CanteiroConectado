import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { buscarCanteiro } from '@/shared/canteiro/api';
import { erroDaApi } from '@/test-utils/api';
import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { atualizarCanteiro, criarCanteiro } from '../api';
import { CanteiroFormTela } from '../telas/CanteiroFormTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ criarCanteiro: jest.fn(), atualizarCanteiro: jest.fn() }));
jest.mock('@/shared/canteiro/api', () => ({
  ...jest.requireActual('@/shared/canteiro/api'),
  buscarCanteiro: jest.fn(),
}));

const mockBack = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: jest.fn(), back: mockBack }) }));

function montar(id?: string) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(administrador))}>
      <CanteiroFormTela id={id} />
    </ComSessao>,
  );
}

describe('CanteiroFormTela (S23)', () => {
  beforeEach(() => mockBack.mockClear());

  it('valida o nome antes de enviar', async () => {
    await montar();
    await fireEvent.press(screen.getByText('Salvar'));
    expect(screen.getByText('Informe o nome do canteiro.')).toBeTruthy();
    expect(criarCanteiro).not.toHaveBeenCalled();
  });

  it('cria o canteiro e volta para a lista', async () => {
    jest
      .mocked(criarCanteiro)
      .mockResolvedValue({ id: 'c1', nome: 'Residencial Aurora', endereco: null, ativo: true });
    await montar();

    await fireEvent.changeText(screen.getByLabelText('Nome'), ' Residencial Aurora ');
    await fireEvent.press(screen.getByText('Salvar'));

    await waitFor(() => expect(mockBack).toHaveBeenCalled());
    expect(criarCanteiro).toHaveBeenCalledWith({ nome: 'Residencial Aurora', endereco: '' });
  });

  it('mostra o nome duplicado no próprio campo', async () => {
    jest.mocked(criarCanteiro).mockRejectedValue(
      erroDaApi(409, {
        codigo: 'CANTEIRO_DUPLICADO',
        detail: 'Já existe um canteiro com este nome.',
        campo: 'nome',
      }),
    );
    await montar();

    await fireEvent.changeText(screen.getByLabelText('Nome'), 'Residencial Aurora');
    await fireEvent.press(screen.getByText('Salvar'));

    expect(await screen.findByText('Já existe um canteiro com este nome.')).toBeTruthy();
    expect(mockBack).not.toHaveBeenCalled();
  });

  it('carrega o canteiro e salva a desativação', async () => {
    jest
      .mocked(buscarCanteiro)
      .mockResolvedValue({ id: 'c1', nome: 'Edifício Sol', endereco: 'Rua B', ativo: true });
    jest
      .mocked(atualizarCanteiro)
      .mockResolvedValue({ id: 'c1', nome: 'Edifício Sol', endereco: 'Rua B', ativo: false });
    await montar('c1');

    expect(await screen.findByDisplayValue('Edifício Sol')).toBeTruthy();
    await fireEvent(screen.getByLabelText('Ativo'), 'valueChange', false);
    await fireEvent.press(screen.getByText('Salvar'));

    await waitFor(() =>
      expect(atualizarCanteiro).toHaveBeenCalledWith('c1', {
        nome: 'Edifício Sol',
        endereco: 'Rua B',
        ativo: false,
      }),
    );
  });
});
