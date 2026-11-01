import { renderHook } from '@testing-library/react-native';

import { useAtualizarAoFocar } from '../useAtualizarAoFocar';

const mockFocos: (() => void)[] = [];
jest.mock('expo-router', () => ({
  useFocusEffect: (callback: () => void) => {
    mockFocos.push(callback);
  },
}));

describe('useAtualizarAoFocar', () => {
  it('não atualiza no primeiro foco e atualiza quando a tela volta a ficar visível', async () => {
    const atualizar = jest.fn();
    await renderHook(() => useAtualizarAoFocar(atualizar));
    const aoFocar = mockFocos[mockFocos.length - 1];

    aoFocar();
    expect(atualizar).not.toHaveBeenCalled();
    aoFocar();
    aoFocar();
    expect(atualizar).toHaveBeenCalledTimes(2);
  });
});
