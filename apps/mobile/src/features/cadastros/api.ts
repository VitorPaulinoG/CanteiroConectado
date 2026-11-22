import { api } from '@/shared/api';
import type { Canteiro } from '@/shared/canteiro';

export type DadosCanteiro = { nome: string; endereco: string };

export async function criarCanteiro(dados: DadosCanteiro): Promise<Canteiro> {
  const resposta = await api.post<Canteiro>('/api/v1/canteiros', dados);
  return resposta.data;
}

export async function atualizarCanteiro(
  id: string,
  dados: DadosCanteiro & { ativo: boolean },
): Promise<Canteiro> {
  const resposta = await api.patch<Canteiro>(`/api/v1/canteiros/${id}`, dados);
  return resposta.data;
}
