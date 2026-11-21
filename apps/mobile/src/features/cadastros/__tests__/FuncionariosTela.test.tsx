import { act, fireEvent, screen } from '@testing-library/react-native';

import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { FuncionariosTela } from '../telas/FuncionariosTela';
import { listarFuncionarios, type Funcionario } from '../usuarios';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../usuarios', () => ({
  ...jest.requireActual('../usuarios'),
  listarFuncionarios: jest.fn(),
}));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ useRouter: () => ({ push: mockPush, back: jest.fn() }) }));

const sebastiao: Funcionario = {
  id: 'u1',
  nome: 'Sebastião Lima',
  email: 'sebastiao@obra.com',
  perfil: 'ALMOXARIFE',
  administrador: false,
  canteiroId: 'c1',
  ativo: true,
  precisaTrocarSenha: true,
  credencialInicialDisponivel: true,
  criadoEm: '2026-11-01T12:00:00Z',
};

function pagina(conteudo: Funcionario[]) {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(administrador))}>
      <FuncionariosTela />
    </ComSessao>,
  );
}

describe('FuncionariosTela (S24)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.mocked(listarFuncionarios).mockReset();
    mockPush.mockClear();
  });
  afterEach(() => jest.useRealTimers());

  it('lista com selos de senha temporária e abre o funcionário', async () => {
    jest.mocked(listarFuncionarios).mockResolvedValue(pagina([sebastiao]));
    await montar();

    expect(await screen.findByText('Sebastião Lima')).toBeTruthy();
    expect(screen.getByText('Senha temporária')).toBeTruthy();
    expect(listarFuncionarios).toHaveBeenCalledWith(
      { q: undefined, perfil: undefined, ativo: true },
      0,
    );

    await fireEvent.press(screen.getByText('Sebastião Lima'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/usuarios/u1');
  });

  it('busca depois da pausa na digitação e filtra pelo perfil', async () => {
    jest.mocked(listarFuncionarios).mockResolvedValue(pagina([]));
    await montar();
    await screen.findByText('Nenhum funcionário');

    await fireEvent.changeText(screen.getByLabelText('Buscar funcionário'), 'seb');
    expect(listarFuncionarios).not.toHaveBeenCalledWith(expect.objectContaining({ q: 'seb' }), 0);
    await act(async () => {
      jest.advanceTimersByTime(300);
    });
    expect(listarFuncionarios).toHaveBeenLastCalledWith(
      { q: 'seb', perfil: undefined, ativo: true },
      0,
    );

    await fireEvent.press(screen.getByText('Gestores'));
    expect(listarFuncionarios).toHaveBeenLastCalledWith(
      { q: 'seb', perfil: 'GESTOR', ativo: true },
      0,
    );
  });

  it('abre o cadastro de novo funcionário', async () => {
    jest.mocked(listarFuncionarios).mockResolvedValue(pagina([]));
    await montar();
    await fireEvent.press(screen.getByText('Novo funcionário'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/usuarios/novo');
  });
});
