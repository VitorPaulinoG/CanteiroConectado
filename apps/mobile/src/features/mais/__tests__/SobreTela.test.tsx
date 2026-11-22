import { screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { buscarPing } from '@/shared/api/ping';

import { SobreTela } from '../telas/SobreTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/api/ping', () => ({ buscarPing: jest.fn() }));
jest.mock('@/shared/utils/appVersion', () => ({ versaoDoApp: () => '1.0.0' }));

const buscarPingMock = jest.mocked(buscarPing);

describe('SobreTela', () => {
  it('mostra a versão do app e a resposta do ping', async () => {
    buscarPingMock.mockResolvedValue({
      nome: 'canteiroconectado',
      versao: '0.0.1-SNAPSHOT',
      horaServidor: '2026-11-01T15:00:00Z',
    });

    await renderizar(<SobreTela />);

    expect(screen.getByText('1.0.0')).toBeTruthy();
    expect(await screen.findByText('0.0.1-SNAPSHOT')).toBeTruthy();
    expect(screen.getByText('canteiroconectado')).toBeTruthy();
  });

  it('mostra o erro com Tentar de novo quando o ping falha', async () => {
    buscarPingMock.mockRejectedValue(new Error('falhou'));

    await renderizar(<SobreTela />);

    expect(await screen.findByText('Tentar de novo')).toBeTruthy();
  });
});
