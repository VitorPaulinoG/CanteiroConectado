import { api, type Pagina } from '@/shared/api';

export type Unidade = 'UN' | 'KG' | 'M' | 'M2' | 'M3' | 'SACO' | 'LATA' | 'CAIXA' | 'LITRO';
export type TipoItem = 'MATERIAL' | 'FERRAMENTA';

export type Item = {
  id: string;
  codigo: string;
  descricao: string;
  unidade: Unidade;
  unidadeSimbolo: string;
  tipo: TipoItem;
  quantidadeMinima: number;
  ativo: boolean;
};

export type FiltroItens = { q?: string; tipo?: TipoItem; ativo?: boolean };

export const UNIDADES: { valor: Unidade; simbolo: string }[] = [
  { valor: 'UN', simbolo: 'un' },
  { valor: 'KG', simbolo: 'kg' },
  { valor: 'M', simbolo: 'm' },
  { valor: 'M2', simbolo: 'm²' },
  { valor: 'M3', simbolo: 'm³' },
  { valor: 'SACO', simbolo: 'saco' },
  { valor: 'LATA', simbolo: 'lata' },
  { valor: 'CAIXA', simbolo: 'caixa' },
  { valor: 'LITRO', simbolo: 'litro' },
];

export const ROTULO_DO_TIPO: Record<TipoItem, string> = {
  MATERIAL: 'Material',
  FERRAMENTA: 'Ferramenta',
};

export const chavesItens = {
  todos: ['itens'] as const,
  lista: (filtro: FiltroItens) => ['itens', 'lista', filtro] as const,
  detalhe: (id: string) => ['itens', 'detalhe', id] as const,
};

export async function listarItens(filtro: FiltroItens, pagina: number): Promise<Pagina<Item>> {
  const resposta = await api.get<Pagina<Item>>('/api/v1/itens', { params: { ...filtro, pagina } });
  return resposta.data;
}

export async function buscarItem(id: string): Promise<Item> {
  return (await api.get<Item>(`/api/v1/itens/${id}`)).data;
}
