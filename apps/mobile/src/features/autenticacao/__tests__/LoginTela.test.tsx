import { fireEvent, screen } from '@testing-library/react-native';
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { SessaoContext, type ValorSessao } from '@/shared/sessao';
import { renderizar } from '@/test-utils/render';

import { LoginTela } from '../telas/LoginTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
const erroDaApi = (status: number, data: object) =>
  new AxiosError('x', 'ERR', config, {}, { status, statusText: '', headers: {}, config, data });

function montar(entrar: ValorSessao['entrar'], aviso?: string) {
  const valor: ValorSessao = {
    estado: { status: 'anonimo', aviso },
    entrar,
    sair: jest.fn(),
    atualizarUsuario: jest.fn(),
    adiarTroca: jest.fn(),
  };
  return renderizar(
    <SafeAreaProvider
      initialMetrics={{
        frame: { x: 0, y: 0, width: 360, height: 640 },
        insets: { top: 0, left: 0, right: 0, bottom: 0 },
      }}
    >
      <SessaoContext.Provider value={valor}>
        <LoginTela />
      </SessaoContext.Provider>
    </SafeAreaProvider>,
  );
}

async function preencher(email: string, senha: string) {
  await fireEvent.changeText(screen.getByLabelText('E-mail'), email);
  await fireEvent.changeText(screen.getByLabelText('Senha'), senha);
  await fireEvent.press(screen.getByText('Entrar'));
}

describe('LoginTela (S02)', () => {
  it('valida os campos antes de enviar', async () => {
    const entrar = jest.fn();
    await montar(entrar);

    await fireEvent.press(screen.getByText('Entrar'));

    expect(screen.getByText('Informe o e-mail.')).toBeTruthy();
    expect(screen.getByText('Informe a senha.')).toBeTruthy();
    expect(entrar).not.toHaveBeenCalled();
  });

  it('envia e-mail e senha', async () => {
    const entrar = jest.fn().mockResolvedValue(undefined);
    await montar(entrar);

    await preencher('sebastiao@obra.com', 'senha123');

    expect(entrar).toHaveBeenCalledWith('sebastiao@obra.com', 'senha123');
  });

  it('mostra "E-mail ou senha incorretos" no campo de senha', async () => {
    const entrar = jest.fn().mockRejectedValue(erroDaApi(401, { codigo: 'CREDENCIAIS_INVALIDAS' }));
    await montar(entrar);

    await preencher('sebastiao@obra.com', 'errada1');

    expect(await screen.findByText('E-mail ou senha incorretos.')).toBeTruthy();
  });

  it('mostra o tempo restante quando a conta está bloqueada', async () => {
    const bloqueadoAte = new Date(Date.now() + 14 * 60_000 + 5_000).toISOString();
    const entrar = jest
      .fn()
      .mockRejectedValue(erroDaApi(423, { codigo: 'CONTA_BLOQUEADA', bloqueadoAte }));
    await montar(entrar);

    await preencher('sebastiao@obra.com', 'errada1');

    expect(
      await screen.findByText(
        'Conta bloqueada por excesso de tentativas. Tente de novo em 15 minutos.',
      ),
    ).toBeTruthy();
  });

  it('avisa quando não há conexão', async () => {
    const entrar = jest
      .fn()
      .mockRejectedValue(new AxiosError('Network Error', 'ERR_NETWORK', config));
    await montar(entrar);

    await preencher('sebastiao@obra.com', 'senha123');

    expect(
      await screen.findByText('Sem conexão com o servidor. Confira a internet e tente de novo.'),
    ).toBeTruthy();
  });

  it('mostra o aviso de sessão encerrada', async () => {
    await montar(jest.fn(), 'Entre de novo para continuar.');
    expect(screen.getByText('Entre de novo para continuar.')).toBeTruthy();
  });

  it('mostra e esconde a senha', async () => {
    await montar(jest.fn());
    expect(screen.getByLabelText('Senha').props.secureTextEntry).toBe(true);
    await fireEvent.press(screen.getByLabelText('Mostrar senha'));
    expect(screen.getByLabelText('Senha').props.secureTextEntry).toBe(false);
  });
});
