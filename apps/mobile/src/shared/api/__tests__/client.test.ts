import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios';

import {
  CABECALHO_REQUEST_ID,
  criarCliente,
  definirAoPerderSessao,
  definirLeitorDeToken,
  ROTA_LOGIN,
  TIMEOUT_MS,
} from '../client';

function adaptadorQueCaptura(capturadas: InternalAxiosRequestConfig[]): AxiosAdapter {
  return async (config) => {
    capturadas.push(config);
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config };
  };
}

describe('criarCliente', () => {
  afterEach(() => definirLeitorDeToken(() => null));

  it('usa a URL base e o timeout de 15 segundos', async () => {
    const cliente = criarCliente('http://10.0.2.2:8080');
    expect(cliente.defaults.baseURL).toBe('http://10.0.2.2:8080');
    expect(cliente.defaults.timeout).toBe(TIMEOUT_MS);
    expect(TIMEOUT_MS).toBe(15_000);
  });

  it('envia um X-Request-Id diferente em cada requisição', async () => {
    const capturadas: InternalAxiosRequestConfig[] = [];
    const cliente = criarCliente('http://api');
    await cliente.get('/a', { adapter: adaptadorQueCaptura(capturadas) });
    await cliente.get('/b', { adapter: adaptadorQueCaptura(capturadas) });

    const [primeiro, segundo] = capturadas.map((c) => c.headers.get(CABECALHO_REQUEST_ID));
    expect(primeiro).toMatch(/^[A-Za-z0-9_-]{1,64}$/);
    expect(primeiro).not.toBe(segundo);
  });

  it('não envia Authorization sem token', async () => {
    const capturadas: InternalAxiosRequestConfig[] = [];
    await criarCliente('http://api').get('/a', { adapter: adaptadorQueCaptura(capturadas) });
    expect(capturadas[0].headers.get('Authorization')).toBeUndefined();
  });

  it('envia o token lido no momento da requisição', async () => {
    const capturadas: InternalAxiosRequestConfig[] = [];
    definirLeitorDeToken(() => 'abc.def.ghi');
    await criarCliente('http://api').get('/a', { adapter: adaptadorQueCaptura(capturadas) });
    expect(capturadas[0].headers.get('Authorization')).toBe('Bearer abc.def.ghi');
  });
});

describe('perda de sessão', () => {
  afterEach(() => definirAoPerderSessao(() => undefined));

  const adaptadorComStatus =
    (status: number): AxiosAdapter =>
    async (config) => {
      const erro = new AxiosError(
        'x',
        'ERR',
        config,
        {},
        {
          status,
          statusText: '',
          headers: {},
          config,
          data: { codigo: 'NAO_AUTENTICADO' },
        },
      );
      throw erro;
    };

  it('avisa quando uma rota autenticada responde 401', async () => {
    const aoPerder = jest.fn();
    definirAoPerderSessao(aoPerder);
    await expect(
      criarCliente('http://api').get('/api/v1/itens', { adapter: adaptadorComStatus(401) }),
    ).rejects.toBeTruthy();
    expect(aoPerder).toHaveBeenCalledTimes(1);
  });

  it('não avisa no 401 do próprio login', async () => {
    const aoPerder = jest.fn();
    definirAoPerderSessao(aoPerder);
    await expect(
      criarCliente('http://api').post(ROTA_LOGIN, {}, { adapter: adaptadorComStatus(401) }),
    ).rejects.toBeTruthy();
    expect(aoPerder).not.toHaveBeenCalled();
  });

  it('não avisa em 403', async () => {
    const aoPerder = jest.fn();
    definirAoPerderSessao(aoPerder);
    await expect(
      criarCliente('http://api').get('/api/v1/itens', { adapter: adaptadorComStatus(403) }),
    ).rejects.toBeTruthy();
    expect(aoPerder).not.toHaveBeenCalled();
  });
});
