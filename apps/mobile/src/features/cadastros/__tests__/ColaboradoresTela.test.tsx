import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { atualizarColaborador, listarColaboradores } from '@/shared/colaborador/api';
import { renderizar } from '@/test-utils/render';
import { autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { ColaboradoresTela } from '../telas/ColaboradoresTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/colaborador/api', () => ({
  ...jest.requireActual('@/shared/colaborador/api'),
  listarColaboradores: jest.fn(),
  atualizarColaborador: jest.fn(),
}));
jest.mock('expo-router', () => ({ useRouter: () => ({ push: jest.fn(), back: jest.fn() }) }));

const joao = { id: 'col-1', nome: 'João da Silva', canteiroId: 'c-aurora', ativo: true };

describe('ColaboradoresTela (S22)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('lista os colaboradores do canteiro ativo e desativa pelo modal', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue({
      conteudo: [joao],
      pagina: 0,
      tamanho: 20,
      totalElementos: 1,
      totalPaginas: 1,
    });
    jest.mocked(atualizarColaborador).mockResolvedValue({ ...joao, ativo: false });
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))}>
        <ColaboradoresTela />
      </ComSessao>,
    );

    await fireEvent.press(await screen.findByText('João da Silva'));
    expect(listarColaboradores).toHaveBeenCalledWith('c-aurora', { q: undefined, ativo: true }, 0);
    expect(screen.getByText('Editar colaborador')).toBeTruthy();
    await fireEvent(screen.getByLabelText('Ativo'), 'valueChange', false);
    await fireEvent.press(screen.getByText('Salvar'));

    await waitFor(() =>
      expect(atualizarColaborador).toHaveBeenCalledWith('col-1', {
        nome: 'João da Silva',
        ativo: false,
      }),
    );
  });

  it('mostra o canteiro ativo no cabeçalho', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue({
      conteudo: [],
      pagina: 0,
      tamanho: 20,
      totalElementos: 0,
      totalPaginas: 0,
    });
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))}>
        <ColaboradoresTela />
      </ComSessao>,
    );
    expect(screen.getByText('Residencial Aurora ▾')).toBeTruthy();
    expect(await screen.findByText('Nenhum colaborador')).toBeTruthy();
  });
});
