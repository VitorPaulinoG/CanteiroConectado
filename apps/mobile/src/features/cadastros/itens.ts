import { api } from '@/shared/api';
import type { Item, TipoItem, Unidade } from '@/shared/item';

export type DadosItem = {
  codigo: string;
  descricao: string;
  unidade: Unidade;
  tipo: TipoItem;
  quantidadeMinima: number;
};

export async function criarItem(dados: DadosItem): Promise<Item> {
  return (await api.post<Item>('/api/v1/itens', dados)).data;
}

export async function atualizarItem(
  id: string,
  dados: Partial<DadosItem> & { ativo?: boolean },
): Promise<Item> {
  return (await api.patch<Item>(`/api/v1/itens/${id}`, dados)).data;
}
