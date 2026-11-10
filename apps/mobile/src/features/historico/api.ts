import { api, type Pagina } from '@/shared/api';
import type { Movimentacao } from '@/shared/estoque';

import type { ParametrosDoHistorico } from './filtros';

export const chavesHistorico = {
  lista: (canteiroId: string, parametros: ParametrosDoHistorico) =>
    ['movimentacoes', 'historico', canteiroId, parametros] as const,
};

export async function listarHistorico(
  canteiroId: string,
  parametros: ParametrosDoHistorico,
  pagina: number,
): Promise<Pagina<Movimentacao>> {
  const resposta = await api.get<Pagina<Movimentacao>>('/api/v1/movimentacoes', {
    params: { canteiroId, ...parametros, pagina },
  });
  return resposta.data;
}
