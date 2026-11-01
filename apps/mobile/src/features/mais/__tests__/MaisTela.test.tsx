import { fireEvent, screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { MaisTela } from '../telas/MaisTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush }) }));

describe('MaisTela (S19)', () => {
  it('mostra iniciais, nome e perfil', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))}>
        <MaisTela />
      </ComSessao>,
    );
    expect(screen.getByText('A')).toBeTruthy();
    expect(screen.getByText('Administrador')).toBeTruthy();
    expect(screen.getByText('Gestor · Administrador')).toBeTruthy();
  });

  it('mostra Canteiros só para o administrador', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))}>
        <MaisTela />
      </ComSessao>,
    );
    await fireEvent.press(screen.getByText('Canteiros'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/canteiros');
  });

  it('esconde a administração do gestor comum', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))}>
        <MaisTela />
      </ComSessao>,
    );
    expect(screen.queryByText('Administração')).toBeNull();
  });

  it('abre Sobre e Trocar senha', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))}>
        <MaisTela />
      </ComSessao>,
    );
    await fireEvent.press(screen.getByText('Sobre'));
    await fireEvent.press(screen.getByText('Trocar senha'));
    expect(mockPush).toHaveBeenCalledWith('/sobre');
    expect(mockPush).toHaveBeenCalledWith('/trocar-senha');
  });

  it('pede confirmação antes de sair', async () => {
    const valor = valorSessao(autenticado(administrador));
    await renderizar(
      <ComSessao valor={valor}>
        <MaisTela />
      </ComSessao>,
    );

    await fireEvent.press(screen.getByText('Sair'));
    expect(screen.getByText('Sair do aplicativo?')).toBeTruthy();
    expect(valor.sair).not.toHaveBeenCalled();

    const botoes = screen.getAllByText('Sair');
    await fireEvent.press(botoes[botoes.length - 1]);
    expect(valor.sair).toHaveBeenCalled();
  });
});
