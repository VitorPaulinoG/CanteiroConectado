import type { AppTheme } from '@/theme';

import type { StatusRequisicao } from './api';

export const ROTULO_DO_STATUS: Record<StatusRequisicao, string> = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  RECUSADA: 'Recusada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
};

export const ICONE_DO_STATUS: Record<StatusRequisicao, string> = {
  PENDENTE: 'clock-outline',
  APROVADA: 'check-circle-outline',
  RECUSADA: 'close-circle-outline',
  ATENDIDA: 'package-variant-closed-check',
  CANCELADA: 'cancel',
};

export const DESCRICAO_DO_EVENTO: Record<StatusRequisicao, string> = {
  PENDENTE: 'Criada',
  APROVADA: 'Aprovada',
  RECUSADA: 'Recusada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
};

export function coresDoStatus(status: StatusRequisicao, theme: AppTheme) {
  switch (status) {
    case 'PENDENTE':
      return {
        fundo: theme.status.belowMinimumContainer,
        texto: theme.status.onBelowMinimumContainer,
      };
    case 'APROVADA':
      return { fundo: theme.status.normalContainer, texto: theme.status.onNormalContainer };
    case 'RECUSADA':
      return { fundo: theme.status.emptyContainer, texto: theme.status.onEmptyContainer };
    case 'ATENDIDA':
      return { fundo: theme.colors.primaryContainer, texto: theme.colors.onPrimaryContainer };
    default:
      return { fundo: theme.colors.surfaceVariant, texto: theme.colors.onSurfaceVariant };
  }
}
