import { fireEvent, screen } from '@testing-library/react-native';
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

import { renderizar } from '@/test-utils/render';
import { administrador, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';

import { postarTrocaDeSenha } from '../api';
import { TrocarSenhaTela } from '../telas/TrocarSenhaTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ postarTrocaDeSenha: jest.fn() }));

const mockReplace = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ replace: mockReplace, back: jest.fn() }),
}));

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
const erroDaApi = (codigo: string, detail?: string) =>
  new AxiosError(
    'x',
    'ERR',
    config,
    {},
    {
      status: 422,
      statusText: '',
      headers: {},
      config,
      data: { codigo, detail },
    },
  );

const pendente = { ...gestor, precisaTrocarSenha: true };

async function preencher(atual: string, nova: string, confirmacao = nova) {
  await fireEvent.changeText(screen.getByLabelText('Senha atual'), atual);
  await fireEvent.changeText(screen.getByLabelText('Nova senha'), nova);
  await fireEvent.changeText(screen.getByLabelText('Confirmar nova senha'), confirmacao);
  await fireEvent.press(screen.getByText('Salvar senha'));
}

describe('TrocarSenhaTela (S03)', () => {
  beforeEach(() => mockReplace.mockClear());

  it('não oferece "Trocar depois" ao funcionário com troca obrigatória', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(pendente))}>
        <TrocarSenhaTela />
      </ComSessao>,
    );
    expect(screen.queryByText('Trocar depois')).toBeNull();
    expect(screen.getByText(/troque a senha temporária/)).toBeTruthy();
  });

  it('deixa o administrador trocar depois', async () => {
    const valor = valorSessao(autenticado({ ...administrador, precisaTrocarSenha: true }));
    await renderizar(
      <ComSessao valor={valor}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await fireEvent.press(screen.getByText('Trocar depois'));

    expect(valor.adiarTroca).toHaveBeenCalled();
    expect(mockReplace).toHaveBeenCalledWith('/inicio');
  });

  it('marca as regras atendidas enquanto digita', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(pendente))}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await fireEvent.changeText(screen.getByLabelText('Nova senha'), 'canteiro');

    expect(screen.getByLabelText('Pelo menos 8 caracteres: atendida')).toBeTruthy();
    expect(screen.getByLabelText('Pelo menos um número: pendente')).toBeTruthy();
  });

  it('valida antes de enviar', async () => {
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(pendente))}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await preencher('temporaria1', 'curta');

    expect(screen.getByText('A nova senha não atende às regras abaixo.')).toBeTruthy();
    expect(postarTrocaDeSenha).not.toHaveBeenCalled();
  });

  it('salva, atualiza a sessão e abre o início', async () => {
    jest.mocked(postarTrocaDeSenha).mockResolvedValue({ ...pendente, precisaTrocarSenha: false });
    const valor = valorSessao(autenticado(pendente));
    await renderizar(
      <ComSessao valor={valor}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await preencher('temporaria1', 'canteiro9');

    await screen.findByText('Salvar senha');
    expect(postarTrocaDeSenha).toHaveBeenCalledWith('temporaria1', 'canteiro9');
    expect(valor.atualizarUsuario).toHaveBeenCalledWith({ ...pendente, precisaTrocarSenha: false });
    expect(mockReplace).toHaveBeenCalledWith('/inicio');
  });

  it('mostra no campo quando a senha atual está errada', async () => {
    jest.mocked(postarTrocaDeSenha).mockRejectedValue(erroDaApi('SENHA_ATUAL_INCORRETA'));
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(pendente))}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await preencher('errada123', 'canteiro9');

    expect(await screen.findByText('A senha atual está incorreta.')).toBeTruthy();
  });

  it('mostra o motivo que a API devolve para senha fraca', async () => {
    jest
      .mocked(postarTrocaDeSenha)
      .mockRejectedValue(erroDaApi('SENHA_FRACA', 'A senha deve ter pelo menos uma letra.'));
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(pendente))}>
        <TrocarSenhaTela />
      </ComSessao>,
    );

    await preencher('temporaria1', 'canteiro9');

    expect(await screen.findByText('A senha deve ter pelo menos uma letra.')).toBeTruthy();
  });
});
