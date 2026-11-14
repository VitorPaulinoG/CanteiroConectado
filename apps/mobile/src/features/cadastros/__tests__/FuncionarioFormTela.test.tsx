import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import * as Clipboard from 'expo-clipboard';

import { listarCanteiros } from '@/shared/canteiro/api';
import { erroDaApi } from '@/test-utils/api';
import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { FuncionarioFormTela } from '../telas/FuncionarioFormTela';
import {
  buscarCredencialInicial,
  buscarFuncionario,
  criarFuncionario,
  desativarFuncionario,
  redefinirSenha,
  type Funcionario,
} from '../usuarios';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../usuarios', () => ({
  ...jest.requireActual('../usuarios'),
  buscarFuncionario: jest.fn(),
  criarFuncionario: jest.fn(),
  atualizarFuncionario: jest.fn(),
  redefinirSenha: jest.fn(),
  desativarFuncionario: jest.fn(),
  ativarFuncionario: jest.fn(),
  buscarCredencialInicial: jest.fn(),
}));
jest.mock('@/shared/canteiro/api', () => ({
  ...jest.requireActual('@/shared/canteiro/api'),
  listarCanteiros: jest.fn(),
}));

const mockReplace = jest.fn();
const mockBack = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: jest.fn(), back: mockBack, replace: mockReplace }),
}));

const camila: Funcionario = {
  id: 'u2',
  nome: 'Camila Souza',
  email: 'camila@obra.com',
  perfil: 'GESTOR',
  administrador: false,
  canteiroId: null,
  ativo: true,
  precisaTrocarSenha: true,
  credencialInicialDisponivel: true,
  criadoEm: '2026-11-01T12:00:00Z',
};

function montar(id?: string) {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(administrador))}>
      <FuncionarioFormTela id={id} />
    </ComSessao>,
  );
}

describe('FuncionarioFormTela (S25)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest
      .mocked(listarCanteiros)
      .mockResolvedValue([{ id: 'c1', nome: 'Residencial Aurora', endereco: null, ativo: true }]);
  });

  it('exige o canteiro para o almoxarife e esconde o campo para o gestor', async () => {
    await montar();
    await fireEvent.changeText(screen.getByLabelText('Nome'), 'Sebastião');
    await fireEvent.changeText(screen.getByLabelText('E-mail'), 'sebastiao@obra.com');
    await fireEvent.press(screen.getByText('Criar funcionário'));
    expect(screen.getByText('Escolha o canteiro do almoxarife.')).toBeTruthy();
    expect(criarFuncionario).not.toHaveBeenCalled();

    await fireEvent.press(screen.getByText('Gestor'));
    expect(screen.queryByLabelText('Canteiro')).toBeNull();
  });

  it('cria o almoxarife no canteiro escolhido e abre a tela dele', async () => {
    jest.mocked(criarFuncionario).mockResolvedValue({ ...camila, id: 'novo-id' });
    await montar();

    await fireEvent.changeText(screen.getByLabelText('Nome'), ' Sebastião ');
    await fireEvent.changeText(screen.getByLabelText('E-mail'), 'sebastiao@obra.com');
    await fireEvent.press(screen.getByLabelText('Escolher canteiro'));
    await fireEvent.press(await screen.findByText('Residencial Aurora'));
    await fireEvent.press(screen.getByText('Criar funcionário'));

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/cadastros/usuarios/novo-id'));
    expect(criarFuncionario).toHaveBeenCalledWith({
      nome: 'Sebastião',
      email: 'sebastiao@obra.com',
      perfil: 'ALMOXARIFE',
      canteiroId: 'c1',
    });
  });

  it('mostra o e-mail duplicado no campo', async () => {
    jest.mocked(criarFuncionario).mockRejectedValue(
      erroDaApi(409, {
        codigo: 'EMAIL_JA_CADASTRADO',
        detail: 'Já existe um funcionário com este e-mail.',
        campo: 'email',
      }),
    );
    await montar();
    await fireEvent.press(screen.getByText('Gestor'));
    await fireEvent.changeText(screen.getByLabelText('Nome'), 'Camila');
    await fireEvent.changeText(screen.getByLabelText('E-mail'), 'camila@obra.com');
    await fireEvent.press(screen.getByText('Criar funcionário'));

    expect(await screen.findByText('Já existe um funcionário com este e-mail.')).toBeTruthy();
  });

  it('mostra a credencial inicial com Copiar', async () => {
    jest.mocked(buscarFuncionario).mockResolvedValue(camila);
    jest
      .mocked(buscarCredencialInicial)
      .mockResolvedValue({ email: 'camila@obra.com', senhaTemporaria: 'Abc23defGH45' });
    await montar('u2');

    expect(await screen.findByText('Senha temporária: Abc23defGH45')).toBeTruthy();
    await fireEvent.press(screen.getByLabelText('Copiar credencial'));
    expect(Clipboard.setStringAsync).toHaveBeenCalledWith(
      'E-mail: camila@obra.com\nSenha temporária: Abc23defGH45',
    );
  });

  it('não mostra o bloco de credencial quando a senha já foi trocada', async () => {
    jest.mocked(buscarFuncionario).mockResolvedValue({
      ...camila,
      precisaTrocarSenha: false,
      credencialInicialDisponivel: false,
    });
    await montar('u2');

    expect(await screen.findByDisplayValue('Camila Souza')).toBeTruthy();
    expect(screen.queryByText('Credencial inicial')).toBeNull();
    expect(buscarCredencialInicial).not.toHaveBeenCalled();
  });

  it('esconde o bloco quando a API responde CREDENCIAL_JA_TROCADA', async () => {
    jest.mocked(buscarFuncionario).mockResolvedValue(camila);
    jest
      .mocked(buscarCredencialInicial)
      .mockRejectedValue(erroDaApi(409, { codigo: 'CREDENCIAL_JA_TROCADA' }));
    await montar('u2');

    await waitFor(() => expect(buscarCredencialInicial).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByText('Credencial inicial')).toBeNull());
  });

  it('pede confirmação para redefinir a senha e para desativar', async () => {
    jest
      .mocked(buscarFuncionario)
      .mockResolvedValue({ ...camila, credencialInicialDisponivel: false });
    jest.mocked(redefinirSenha).mockResolvedValue(camila);
    jest.mocked(desativarFuncionario).mockResolvedValue({ ...camila, ativo: false });
    await montar('u2');
    await screen.findByDisplayValue('Camila Souza');

    await fireEvent.press(screen.getByText('Redefinir senha'));
    expect(screen.getByText('Redefinir a senha?')).toBeTruthy();
    expect(redefinirSenha).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByText('Redefinir'));
    await waitFor(() => expect(redefinirSenha).toHaveBeenCalledWith('u2'));

    await fireEvent.press(screen.getByText('Desativar'));
    expect(screen.getByText('Desativar o funcionário?')).toBeTruthy();
    const botoes = screen.getAllByText('Desativar');
    await fireEvent.press(botoes[botoes.length - 1]);
    await waitFor(() => expect(desativarFuncionario).toHaveBeenCalledWith('u2'));
  });
});
