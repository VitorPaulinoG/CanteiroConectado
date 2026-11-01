import { create, type AxiosInstance } from 'axios';

import { ambiente } from '@/config';
import { gerarRequestId } from '@/shared/utils/requestId';

export const TIMEOUT_MS = 15_000;
export const CABECALHO_REQUEST_ID = 'X-Request-Id';

type LeitorDeToken = () => string | null;

let lerToken: LeitorDeToken = () => null;

export function definirLeitorDeToken(leitor: LeitorDeToken) {
  lerToken = leitor;
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

  return cliente;
}

export const api = criarCliente(ambiente.valido ? ambiente.apiUrl : 'http://api-nao-configurada');
