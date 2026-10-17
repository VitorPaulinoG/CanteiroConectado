import { render } from '@testing-library/react-native';

import TelaInicial from '../index';

describe('TelaInicial', () => {
  it('mostra o nome do app', async () => {
    const { getByText } = await render(<TelaInicial />);

    expect(getByText('Canteiro Conectado')).toBeTruthy();
  });
});
