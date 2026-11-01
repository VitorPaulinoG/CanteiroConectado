import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import { FILTROS_VAZIOS, type FiltrosDoHistorico } from './filtros';

const CHAVE = ['historico', 'filtros'] as const;

export function useFiltrosDoHistorico() {
  const queryClient = useQueryClient();
  const { data } = useQuery({
    queryKey: CHAVE,
    queryFn: () => FILTROS_VAZIOS,
    initialData: FILTROS_VAZIOS,
    staleTime: Infinity,
    gcTime: Infinity,
  });

  const mudar = useCallback(
    (mudanca: Partial<FiltrosDoHistorico>) => {
      queryClient.setQueryData<FiltrosDoHistorico>(CHAVE, (atuais) => ({
        ...(atuais ?? FILTROS_VAZIOS),
        ...mudanca,
      }));
    },
    [queryClient],
  );

  const limpar = useCallback(() => {
    queryClient.setQueryData(CHAVE, FILTROS_VAZIOS);
  }, [queryClient]);

  return { filtros: data, mudar, limpar };
}
