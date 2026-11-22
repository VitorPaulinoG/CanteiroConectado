import { render, screen } from '@testing-library/react-native';
import { BottomTabBarHeightContext } from 'expo-router/tabs';
import type { ReactElement } from 'react';
import { Text } from 'react-native';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { appTheme } from '@/theme';

import { TelaComCabecalho } from '../TelaComCabecalho';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const BARRA_DO_SISTEMA = 48;

const montar = (ui: ReactElement) =>
  render(
    <SafeAreaProvider
      initialMetrics={{
        frame: { x: 0, y: 0, width: 360, height: 640 },
        insets: { top: 24, left: 0, right: 0, bottom: BARRA_DO_SISTEMA },
      }}
    >
      <PaperProvider theme={appTheme}>{ui}</PaperProvider>
    </SafeAreaProvider>,
  );

const tela = (
  <TelaComCabecalho titulo="Tela">
    <Text>Conteúdo</Text>
  </TelaComCabecalho>
);

describe('TelaComCabecalho', () => {
  it('reserva o espaço da barra de navegação do sistema quando a tela fica fora das abas', async () => {
    await montar(tela);

    expect(screen.getByTestId('conteudo-da-tela')).toHaveStyle({
      marginBottom: BARRA_DO_SISTEMA,
    });
  });

  it('não reserva o espaço de novo quando a tela fica dentro das abas', async () => {
    await montar(
      <BottomTabBarHeightContext.Provider value={80}>{tela}</BottomTabBarHeightContext.Provider>,
    );

    expect(screen.getByTestId('conteudo-da-tela')).toHaveStyle({ marginBottom: 0 });
  });
});
