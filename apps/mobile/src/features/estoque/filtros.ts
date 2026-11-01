import type { StatusEstoque } from '@/shared/estoque';

export type FiltroDeStatus = 'todos' | 'alerta' | 'zerados';

export const FILTROS_DE_STATUS: { valor: FiltroDeStatus; rotulo: string }[] = [
  { valor: 'todos', rotulo: 'Todos' },
  { valor: 'alerta', rotulo: 'Abaixo do mínimo' },
  { valor: 'zerados', rotulo: 'Zerados' },
];

export function statusDoFiltro(filtro: FiltroDeStatus): StatusEstoque[] | undefined {
  if (filtro === 'alerta') {
    return ['ZERADO', 'ABAIXO_DO_MINIMO'];
  }
  return filtro === 'zerados' ? ['ZERADO'] : undefined;
}

export function lerFiltro(valor: string | undefined): FiltroDeStatus {
  return valor === 'alerta' || valor === 'zerados' ? valor : 'todos';
}
