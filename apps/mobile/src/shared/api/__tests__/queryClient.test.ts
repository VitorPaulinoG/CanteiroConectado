import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

import {
  criarQueryClient,
  deveRepetirConsulta,
  deveRepetirMutacao,
  esperaCrescente,
} from '../queryClient';

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
const semRede = new AxiosError('Network Error', 'ERR_NETWORK', config);
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

describe('deveRepetirConsulta', () => {
  it('repete até 2 vezes em falha de rede ou erro do servidor', () => {
    expect(deveRepetirConsulta(0, semRede)).toBe(true);
    expect(deveRepetirConsulta(1, comStatus(503))).toBe(true);
    expect(deveRepetirConsulta(2, semRede)).toBe(false);
  });

  it('nunca repete resposta 4xx', () => {
    expect(deveRepetirConsulta(0, comStatus(404))).toBe(false);
    expect(deveRepetirConsulta(0, comStatus(422))).toBe(false);
  });
});

describe('deveRepetirMutacao', () => {
  it('repete uma vez só quando não houve resposta', () => {
    expect(deveRepetirMutacao(0, semRede)).toBe(true);
    expect(deveRepetirMutacao(1, semRede)).toBe(false);
  });

  it('não repete quando o servidor respondeu', () => {
    expect(deveRepetirMutacao(0, comStatus(500))).toBe(false);
    expect(deveRepetirMutacao(0, comStatus(422))).toBe(false);
  });
});

describe('esperaCrescente', () => {
  it('dobra a espera a cada tentativa com teto de 8 segundos', () => {
    expect(esperaCrescente(0)).toBe(1_000);
    expect(esperaCrescente(1)).toBe(2_000);
    expect(esperaCrescente(5)).toBe(8_000);
  });
});

describe('criarQueryClient', () => {
  it('usa staleTime de 30 segundos', () => {
    const cliente = criarQueryClient();
    expect(cliente.getDefaultOptions().queries?.staleTime).toBe(30_000);
  });
});
