import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

import { acordarServidor } from '../acordarServidor';

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
const semResposta = () => new AxiosError('Network Error', 'ERR_NETWORK', config);
const comStatus = (status: number) =>
  new AxiosError(
    'x',
    'ERR',
    config,
    {},
    {
      status,
      statusText: '',
      headers: {},
      config,
      data: { codigo: 'X' },
    },
  );

function relogioFalso() {
  let atual = 0;
  return {
    agora: () => atual,
    dormir: async (ms: number) => {
      atual += ms;
    },
    avancar: (ms: number) => {
      atual += ms;
    },
  };
}

describe('acordarServidor', () => {
  it('resolve na primeira resposta', async () => {
    const chamar = jest.fn().mockResolvedValue({});
    await acordarServidor({ chamar });
    expect(chamar).toHaveBeenCalledTimes(1);
  });

  it('repete enquanto o servidor não responde e resolve quando ele acorda', async () => {
    const relogio = relogioFalso();
    const chamar = jest
      .fn()
      .mockRejectedValueOnce(semResposta())
      .mockRejectedValueOnce(comStatus(503))
      .mockResolvedValue({});

    await acordarServidor({ chamar, agora: relogio.agora, dormir: relogio.dormir });

    expect(chamar).toHaveBeenCalledTimes(3);
  });

  it('desiste depois de 90 segundos', async () => {
    const relogio = relogioFalso();
    const chamar = jest.fn().mockImplementation(async () => {
      relogio.avancar(15_000);
      throw semResposta();
    });

    await expect(
      acordarServidor({ chamar, agora: relogio.agora, dormir: relogio.dormir }),
    ).rejects.toMatchObject({ codigo: 'SEM_CONEXAO' });
    expect(relogio.agora()).toBeLessThanOrEqual(90_000);
    expect(chamar.mock.calls.length).toBeGreaterThan(1);
  });

  it('não repete quando o servidor responde com erro 4xx', async () => {
    const chamar = jest.fn().mockRejectedValue(comStatus(404));
    await expect(acordarServidor({ chamar })).rejects.toMatchObject({ status: 404 });
    expect(chamar).toHaveBeenCalledTimes(1);
  });
});
