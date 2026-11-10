import type { TipoMovimentacao } from '@/shared/estoque';
import { deData, paraData } from '@/shared/utils/data';

export type Periodo = 'tudo' | 'hoje' | '7dias' | 'personalizado';

export type FiltrosDoHistorico = {
  periodo: Periodo;
  de: string | null;
  ate: string | null;
  tipo: TipoMovimentacao | null;
  item: { itemId: string; descricao: string } | null;
  colaborador: { id: string; nome: string } | null;
};

export const FILTROS_VAZIOS: FiltrosDoHistorico = {
  periodo: 'tudo',
  de: null,
  ate: null,
  tipo: null,
  item: null,
  colaborador: null,
};

export type ParametrosDoHistorico = {
  de?: string;
  ate?: string;
  tipo?: TipoMovimentacao;
  itemId?: string;
  colaboradorId?: string;
};

function inicioDoDia(data: Date): Date {
  const inicio = new Date(data);
  inicio.setHours(0, 0, 0, 0);
  return inicio;
}

function diasDepois(data: Date, dias: number): Date {
  const depois = new Date(data);
  depois.setDate(depois.getDate() + dias);
  return depois;
}

export function intervaloDoPeriodo(
  filtros: Pick<FiltrosDoHistorico, 'periodo' | 'de' | 'ate'>,
  agora: Date = new Date(),
): { de?: Date; ate?: Date } {
  const hoje = inicioDoDia(agora);
  switch (filtros.periodo) {
    case 'hoje':
      return { de: hoje, ate: diasDepois(hoje, 1) };
    case '7dias':
      return { de: diasDepois(hoje, -6), ate: diasDepois(hoje, 1) };
    case 'personalizado':
      return {
        de: filtros.de ? paraData(filtros.de) : undefined,
        ate: filtros.ate ? diasDepois(paraData(filtros.ate), 1) : undefined,
      };
    default:
      return {};
  }
}

export function paraParametros(
  filtros: FiltrosDoHistorico,
  agora: Date = new Date(),
): ParametrosDoHistorico {
  const { de, ate } = intervaloDoPeriodo(filtros, agora);
  return {
    de: de?.toISOString(),
    ate: ate?.toISOString(),
    tipo: filtros.tipo ?? undefined,
    itemId: filtros.item?.itemId,
    colaboradorId: filtros.colaborador?.id,
  };
}

export function quantosFiltrosExtras(filtros: FiltrosDoHistorico): number {
  return [filtros.tipo, filtros.item, filtros.colaborador].filter(Boolean).length;
}

export function temFiltros(filtros: FiltrosDoHistorico): boolean {
  return filtros.periodo !== 'tudo' || quantosFiltrosExtras(filtros) > 0;
}

export function rotuloDoDia(iso: string, agora: Date = new Date()): string {
  const dia = deData(new Date(iso));
  const hoje = deData(agora);
  const ontem = deData(diasDepois(agora, -1));
  if (dia === hoje) {
    return 'Hoje';
  }
  if (dia === ontem) {
    return 'Ontem';
  }
  const [ano, mes, numero] = dia.split('-');
  return `${numero}/${mes}/${ano}`;
}
