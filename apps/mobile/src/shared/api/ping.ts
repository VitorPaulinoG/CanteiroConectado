import type { AxiosRequestConfig } from 'axios';

import { api } from './client';

export type Ping = {
  nome: string;
  versao: string;
  horaServidor: string;
};

export async function buscarPing(config?: AxiosRequestConfig): Promise<Ping> {
  const resposta = await api.get<Ping>('/api/v1/ping', config);
  return resposta.data;
}
