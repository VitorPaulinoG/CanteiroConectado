import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;

export function erroDaApi(status: number, data: Record<string, unknown>) {
  return new AxiosError(
    'falhou',
    'ERR_BAD_REQUEST',
    config,
    {},
    {
      status,
      statusText: '',
      headers: {},
      config,
      data,
    },
  );
}

export function erroDeRede() {
  return new AxiosError('Network Error', 'ERR_NETWORK', config);
}

export function erroDeTempo() {
  return new AxiosError('timeout', 'ECONNABORTED', config);
}
