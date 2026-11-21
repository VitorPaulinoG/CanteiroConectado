import type { StockStatus } from '@/shared/components/StatusBadge';

import type { StatusEstoque } from './api';

const PARA_SELO: Record<StatusEstoque, StockStatus> = {
  ZERADO: 'empty',
  ABAIXO_DO_MINIMO: 'belowMinimum',
  NORMAL: 'normal',
};

export function seloDoStatus(status: StatusEstoque): StockStatus {
  return PARA_SELO[status];
}
