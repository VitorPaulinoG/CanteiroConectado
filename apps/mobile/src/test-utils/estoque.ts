import type { ItemEstoque, Movimentacao } from '@/shared/estoque';
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

export const saidaGravada: Movimentacao = {
  id: 'm-saida',
  canteiroId: 'c-aurora',
  canteiroNome: 'Residencial Aurora',
  itemId: 'i-furadeira',
  itemCodigo: 'FER-0012',
  itemDescricao: 'Furadeira de impacto 700W',
  unidadeSimbolo: 'un',
  tipo: 'SAIDA',
  motivo: 'RETIRADA',
  quantidade: 1,
  saldoApos: 1,
  colaboradorId: 'col-joao',
  colaboradorNome: 'João da Silva',
  frenteObra: 'Fundação',
  observacao: null,
  estornaId: null,
  estornadaPorId: null,
  registradoPorId: 'u-almoxarife',
  registradoPorNome: 'Sebastião Lima',
  criadoEm: '2026-11-01T15:30:00Z',
};
