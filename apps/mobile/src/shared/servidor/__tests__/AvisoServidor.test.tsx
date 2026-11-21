import { act, render, screen } from '@testing-library/react-native';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { appTheme } from '@/theme';

import { AvisoServidor } from '../AvisoServidor';
import { ServidorProvider } from '../ServidorProvider';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

function montar(chamar: () => Promise<unknown>) {
  return render(
    <SafeAreaProvider
      initialMetrics={{
        frame: { x: 0, y: 0, width: 360, height: 640 },
        insets: { top: 0, left: 0, right: 0, bottom: 0 },
      }}
    >
      <PaperProvider theme={appTheme}>
        <ServidorProvider chamar={chamar}>
          <AvisoServidor />
        </ServidorProvider>
      </PaperProvider>
    </SafeAreaProvider>,
  );
}

describe('AvisoServidor', () => {
  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  it('não mostra nada quando o servidor responde rápido', async () => {
    await montar(() => Promise.resolve({}));
    await act(async () => {
      jest.advanceTimersByTime(9_000);
    });
    expect(screen.queryByText(/servidor está acordando/)).toBeNull();
  });

  it('mostra o aviso quando a primeira resposta passa de 8 segundos', async () => {
    await montar(() => new Promise(() => undefined));
    expect(screen.queryByText(/servidor está acordando/)).toBeNull();
    await act(async () => {
      jest.advanceTimersByTime(8_000);
    });
    expect(screen.getByText(/servidor está acordando/)).toBeTruthy();
  });
});
