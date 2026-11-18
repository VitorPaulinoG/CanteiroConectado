import type { ItemEstoque } from '@/shared/estoque';
import type { Colaborador } from '@/shared/colaborador';

export const furadeiraNoEstoque: ItemEstoque = {
  itemId: 'i-furadeira',
  codigo: 'FER-0012',
  descricao: 'Furadeira de impacto 700W',
  unidade: 'UN',
  unidadeSimbolo: 'un',
  tipo: 'FERRAMENTA',
  quantidadeMinima: 1,
  saldo: 2,
  status: 'NORMAL',
};

export const joao: Colaborador = {
  id: 'col-joao',
  nome: 'João da Silva',
  canteiroId: 'c-aurora',
  ativo: true,
};

export function pagina<T>(conteudo: T[]) {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}
