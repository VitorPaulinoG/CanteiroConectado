import { api } from '@/shared/api';

export type Canteiro = {
  id: string;
  nome: string;
  endereco: string | null;
  ativo: boolean;
};

export const chavesCanteiros = {
  todos: ['canteiros'] as const,
  lista: (ativo?: boolean) => ['canteiros', 'lista', ativo ?? 'todos'] as const,
  detalhe: (id: string) => ['canteiros', 'detalhe', id] as const,
};

export async function listarCanteiros(ativo?: boolean): Promise<Canteiro[]> {
  const resposta = await api.get<Canteiro[]>('/api/v1/canteiros', {
    params: ativo === undefined ? undefined : { ativo },
  });
  return resposta.data;
}

export async function buscarCanteiro(id: string): Promise<Canteiro> {
  const resposta = await api.get<Canteiro>(`/api/v1/canteiros/${id}`);
  return resposta.data;
}
