import { render } from '@testing-library/react-native';
import { PaperProvider } from 'react-native-paper';

import TelaInicial from '../../app/index';
import { appTheme } from '@/theme';

describe('TelaInicial', () => {
  it('mostra o nome do app', async () => {
    const { getByText } = await render(
      <PaperProvider theme={appTheme}>
        <TelaInicial />
      </PaperProvider>,
    );

    expect(getByText('Canteiro Conectado')).toBeTruthy();
  });
});
