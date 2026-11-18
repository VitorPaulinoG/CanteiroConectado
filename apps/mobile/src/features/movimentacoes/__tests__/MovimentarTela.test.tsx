import { fireEvent, screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { MovimentarTela } from '../telas/MovimentarTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush }) }));

describe('MovimentarTela (S08)', () => {
  it('abre a saída e a entrada pelos dois cartões', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(almoxarife))}>
        <MovimentarTela />
      </ComSessao>,
    );

    await fireEvent.press(screen.getByLabelText('Registrar saída'));
    await fireEvent.press(screen.getByLabelText('Registrar entrada'));

    expect(mockPush).toHaveBeenNthCalledWith(1, '/movimentar/saida');
    expect(mockPush).toHaveBeenNthCalledWith(2, '/movimentar/entrada');
  });
});
