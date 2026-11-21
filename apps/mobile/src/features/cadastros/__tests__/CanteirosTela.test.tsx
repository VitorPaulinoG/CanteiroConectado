import { fireEvent, screen } from '@testing-library/react-native';

import { listarCanteiros } from '@/shared/canteiro/api';
import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { CanteirosTela } from '../telas/CanteirosTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/canteiro/api', () => ({
  ...jest.requireActual('@/shared/canteiro/api'),
  listarCanteiros: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush, back: jest.fn() }) }));

describe('CanteirosTela (S23)', () => {
  beforeEach(() => mockPush.mockClear());

  it('lista os canteiros com o selo de situação e abre o formulário', async () => {
    jest
      .mocked(listarCanteiros)
      .mockResolvedValue([
        { id: 'c1', nome: 'Residencial Aurora', endereco: 'Rua A, 10', ativo: true },
      ]);
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))}>
        <CanteirosTela />
      </ComSessao>,
    );

    expect(await screen.findByText('Residencial Aurora')).toBeTruthy();
    expect(screen.getByText('Rua A, 10')).toBeTruthy();
    expect(screen.getByText('Ativo')).toBeTruthy();
    expect(listarCanteiros).toHaveBeenCalledWith(true);

    await fireEvent.press(screen.getByText('Residencial Aurora'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/canteiros/c1');
    await fireEvent.press(screen.getByText('Novo canteiro'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/canteiros/novo');
  });

  it('troca o filtro para inativos', async () => {
    jest.mocked(listarCanteiros).mockResolvedValue([]);
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))}>
        <CanteirosTela />
      </ComSessao>,
    );
    await screen.findByText('Nenhum canteiro');

    await fireEvent.press(screen.getByText('Inativos'));

    expect(listarCanteiros).toHaveBeenLastCalledWith(false);
  });

  it('mostra acesso negado para quem não é administrador', async () => {
    jest.mocked(listarCanteiros).mockResolvedValue([]);
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))}>
        <CanteirosTela />
      </ComSessao>,
    );
    expect(screen.getByText('Você não tem acesso a esta área')).toBeTruthy();
  });
});
