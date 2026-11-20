import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { pagina } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { listarAuditoria, type EventoDeAuditoria } from '../api';
import { AuditoriaTela, filtroDaAuditoria } from '../telas/AuditoriaTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ ...jest.requireActual('../api'), listarAuditoria: jest.fn() }));
jest.mock('expo-router', () => ({ useRouter: () => ({ back: jest.fn() }) }));

const loginFalhou: EventoDeAuditoria = {
  id: 'e1',
  acao: 'LOGIN_FALHOU',
  usuarioId: 'u-almoxarife',
  usuarioNome: 'Sebastião Lima',
  entidade: 'usuario',
  entidadeId: 'u-almoxarife',
  entidadeDescricao: 'Sebastião Lima',
  detalhes: { email: 'sebastiao@obra.com', motivo: 'SENHA_INCORRETA' },
  requestId: 'req-123',
  criadoEm: '2026-11-01T15:00:00Z',
};

const senhaRedefinida: EventoDeAuditoria = {
  id: 'e2',
  acao: 'SENHA_REDEFINIDA',
  usuarioId: 'u-admin',
  usuarioNome: 'Administrador',
  entidade: 'usuario',
  entidadeId: 'u-gestor',
  entidadeDescricao: 'Camila Souza',
  detalhes: { email: 'camila@obra.com' },
  requestId: 'req-456',
  criadoEm: '2026-11-01T16:00:00Z',
};

function montar(usuario: UsuarioSessao = gestor) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <AuditoriaTela />
    </ComSessao>,
  );
}

describe('AuditoriaTela (S18)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('lista ação, usuário e horário e abre o detalhe com alvo, motivo e requisição', async () => {
    jest.mocked(listarAuditoria).mockResolvedValue(pagina([senhaRedefinida, loginFalhou]));
    await montar();

    expect(await screen.findByText('Senha redefinida')).toBeTruthy();
    expect(screen.getByText('Login com falha')).toBeTruthy();
    expect(screen.getByText(/^Administrador · /)).toBeTruthy();

    await fireEvent.press(screen.getByText('Senha redefinida'));
    expect(await screen.findByText('Camila Souza')).toBeTruthy();
    expect(screen.getByText('req-456')).toBeTruthy();

    await fireEvent.press(screen.getByText('Login com falha'));
    expect(await screen.findByText('SENHA_INCORRETA')).toBeTruthy();
    expect(screen.getByText('req-123')).toBeTruthy();
  });

  it('filtra pela ação e pelo período', async () => {
    jest.mocked(listarAuditoria).mockResolvedValue(pagina([]));
    await montar();
    expect(await screen.findByText('Nenhum evento')).toBeTruthy();

    await fireEvent.press(screen.getByLabelText('Filtrar por ação'));
    await fireEvent.press(await screen.findByText('Login com falha'));
    await fireEvent.press(screen.getByText('Hoje'));

    await waitFor(() =>
      expect(listarAuditoria).toHaveBeenLastCalledWith(
        { acao: 'LOGIN_FALHOU', de: expect.stringMatching(/Z$/) },
        0,
      ),
    );
  });

  it('não oferece nenhuma ação de alterar o evento', async () => {
    jest.mocked(listarAuditoria).mockResolvedValue(pagina([loginFalhou]));
    await montar();
    await fireEvent.press(await screen.findByText('Login com falha'));
    expect(screen.queryByText(/Editar|Excluir|Apagar/)).toBeNull();
  });

  it('mostra acesso negado ao ALMOXARIFE', async () => {
    await montar(almoxarife);
    expect(screen.getByText('Você não tem acesso a esta área')).toBeTruthy();
    expect(listarAuditoria).not.toHaveBeenCalled();
  });
});

describe('filtroDaAuditoria', () => {
  it('converte o período para o começo do dia local em UTC', () => {
    const agora = new Date(2026, 10, 5, 14);
    expect(filtroDaAuditoria(null, 'tudo', agora)).toEqual({ acao: undefined });
    expect(filtroDaAuditoria('LOGIN_FALHOU', 'hoje', agora)).toEqual({
      acao: 'LOGIN_FALHOU',
      de: new Date(2026, 10, 5).toISOString(),
    });
    expect(filtroDaAuditoria(null, '7dias', agora).de).toBe(new Date(2026, 9, 30).toISOString());
  });
});
