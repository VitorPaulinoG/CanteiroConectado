import type { MotivoMovimentacao, TipoMovimentacao } from './api';

export const ROTULO_DO_MOTIVO: Record<MotivoMovimentacao, string> = {
  RECEBIMENTO: 'Recebimento',
  DEVOLUCAO: 'Devolução',
  RETIRADA: 'Retirada',
  AJUSTE: 'Ajuste',
  ESTORNO: 'Estorno',
};

export const ROTULO_DO_TIPO_DE_MOVIMENTACAO: Record<TipoMovimentacao, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
};

export const ICONE_DO_TIPO_DE_MOVIMENTACAO: Record<TipoMovimentacao, string> = {
  ENTRADA: 'arrow-down-bold-circle-outline',
  SAIDA: 'arrow-up-bold-circle-outline',
};
