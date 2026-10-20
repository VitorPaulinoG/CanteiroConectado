import { fireEvent, render, screen } from '@testing-library/react-native';
import { PaperProvider } from 'react-native-paper';
import type { ReactElement } from 'react';

import { appTheme } from '@/theme';
import { ErrorState, QuantityStepper, StatusBadge } from '@/shared';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const renderWithTheme = (ui: ReactElement) =>
  render(<PaperProvider theme={appTheme}>{ui}</PaperProvider>);

describe('StatusBadge', () => {
  it.each([
    ['normal', 'Normal'],
    ['belowMinimum', 'Abaixo do mínimo'],
    ['empty', 'Zerado'],
  ] as const)('mostra texto para %s', async (status, label) => {
    await renderWithTheme(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeTruthy();
  });
});

describe('QuantityStepper', () => {
  it('respeita os limites', async () => {
    const onChange = jest.fn();
    await renderWithTheme(<QuantityStepper value={10} min={0} max={10} onChange={onChange} />);
    await fireEvent.press(screen.getByLabelText('Aumentar quantidade'));
    expect(onChange).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByLabelText('Diminuir quantidade'));
    expect(onChange).toHaveBeenCalledWith(9);
  });
});

describe('ErrorState', () => {
  it('chama onRetry', async () => {
    const onRetry = jest.fn();
    await renderWithTheme(<ErrorState onRetry={onRetry} />);
    await fireEvent.press(screen.getByText('Tentar de novo'));
    expect(onRetry).toHaveBeenCalled();
  });
});
