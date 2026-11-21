import { create, isAxiosError, type AxiosInstance } from 'axios';

import { ambiente } from '@/config';
import { gerarRequestId } from '@/shared/utils/requestId';

export const TIMEOUT_MS = 15_000;
export const CABECALHO_REQUEST_ID = 'X-Request-Id';

export const ROTA_LOGIN = '/api/v1/autenticacao/login';

type LeitorDeToken = () => string | null;

let lerToken: LeitorDeToken = () => null;
let aoPerderSessao: () => void = () => undefined;

export function definirLeitorDeToken(leitor: LeitorDeToken) {
  lerToken = leitor;
}

export function definirAoPerderSessao(callback: () => void) {
  aoPerderSessao = callback;
}

export function criarCliente(baseURL: string): AxiosInstance {
  const cliente = create({
    baseURL,
    timeout: TIMEOUT_MS,
    headers: { Accept: 'application/json' },
  });

  cliente.interceptors.request.use((config) => {
    config.headers.set(CABECALHO_REQUEST_ID, gerarRequestId());
    const token = lerToken();
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
    return config;
  });

  cliente.interceptors.response.use(undefined, (erro: unknown) => {
    if (
      isAxiosError(erro) &&
      erro.response?.status === 401 &&
      !erro.config?.url?.endsWith(ROTA_LOGIN)
    ) {
      aoPerderSessao();
    }
    return Promise.reject(erro);
  });

  return cliente;
}

export const api = criarCliente(ambiente.valido ? ambiente.apiUrl : 'http://api-nao-configurada');
