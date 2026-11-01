import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactElement } from 'react';
import { PaperProvider } from 'react-native-paper';

import { paraAppError } from '@/shared/api';
import { erroDaApi, erroDeRede, erroDeTempo } from '@/test-utils/api';
import { appTheme } from '@/theme';

import { FaixaEstadoEnvio } from '../FaixaEstadoEnvio';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const montar = (ui: ReactElement) => render(<PaperProvider theme={appTheme}>{ui}</PaperProvider>);

describe('FaixaEstadoEnvio', () => {
  it('não mostra nada antes do envio', async () => {
    await montar(<FaixaEstadoEnvio estado="parado" />);
    expect(screen.queryByText('Enviando…')).toBeNull();
    expect(screen.queryByText('Salvo no servidor')).toBeNull();
    expect(screen.queryByText(/Não foi possível enviar/)).toBeNull();
  });

  it('mostra Enviando… enquanto espera a resposta', async () => {
    await montar(<FaixaEstadoEnvio estado="enviando" />);
    expect(screen.getByText('Enviando…')).toBeTruthy();
    expect(screen.queryByText('Salvo no servidor')).toBeNull();
  });

  it('só mostra Salvo no servidor no estado salvo', async () => {
    await montar(<FaixaEstadoEnvio estado="salvo" />);
    expect(screen.getByText('Salvo no servidor')).toBeTruthy();
  });

  it.each([
    ['falha de rede', erroDeRede()],
    ['resposta perdida por tempo esgotado', erroDeTempo()],
  ])('oferece Tentar de novo na %s', async (_caso, falha) => {
    const aoTentarDeNovo = jest.fn();
    await montar(
      <FaixaEstadoEnvio
        estado="falhou"
        erro={paraAppError(falha)}
        aoTentarDeNovo={aoTentarDeNovo}
      />,
    );

    expect(screen.getByText(/Não foi possível enviar/)).toBeTruthy();
    await fireEvent.press(screen.getByText('Tentar de novo'));
    expect(aoTentarDeNovo).toHaveBeenCalled();
  });

  it('no erro 422 mostra a mensagem e oferece Corrigir, sem Tentar de novo', async () => {
    const aoCorrigir = jest.fn();
    const erro = paraAppError(
      erroDaApi(422, {
        codigo: 'SALDO_INSUFICIENTE',
        detail: 'Saldo insuficiente: há 1 no canteiro.',
      }),
    );
    await montar(
      <FaixaEstadoEnvio
        estado="falhou"
        erro={erro}
        aoTentarDeNovo={jest.fn()}
        aoCorrigir={aoCorrigir}
      />,
    );

    expect(screen.getByText('Saldo insuficiente: há 1 no canteiro.')).toBeTruthy();
    expect(screen.queryByText('Tentar de novo')).toBeNull();
    await fireEvent.press(screen.getByText('Corrigir'));
    expect(aoCorrigir).toHaveBeenCalled();
  });
});
