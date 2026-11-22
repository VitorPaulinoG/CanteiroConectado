import { useQuery, useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

import { useUsuario } from '@/shared/sessao';

import { buscarCanteiro, chavesCanteiros, listarCanteiros, type Canteiro } from './api';
import { lerCanteiroAtivo, salvarCanteiroAtivo } from './storage';

export type StatusCanteiroAtivo = 'carregando' | 'pronto' | 'escolher' | 'erro';

export type ValorCanteiroAtivo = {
  status: StatusCanteiroAtivo;
  canteiroId: string | null;
  canteiro: Canteiro | undefined;
  podeEscolher: boolean;
  escolher: (canteiroId: string) => Promise<void>;
  tentarDeNovo: () => void;
};

export const CanteiroAtivoContext = createContext<ValorCanteiroAtivo | null>(null);

export function useCanteiroAtivo(): ValorCanteiroAtivo {
  const valor = useContext(CanteiroAtivoContext);
  if (!valor) {
    throw new Error('useCanteiroAtivo precisa estar dentro de um CanteiroAtivoProvider.');
  }
  return valor;
}

export function useCanteiroIdAtivo(): string {
  const { canteiroId } = useCanteiroAtivo();
  if (!canteiroId) {
    throw new Error('Nenhum canteiro ativo escolhido.');
  }
  return canteiroId;
}

export function CanteiroAtivoProvider({ children }: { children: ReactNode }) {
  const usuario = useUsuario();
  return usuario.perfil === 'ALMOXARIFE' && usuario.canteiroId ? (
    <CanteiroDoAlmoxarife canteiroId={usuario.canteiroId}>{children}</CanteiroDoAlmoxarife>
  ) : (
    <CanteiroEscolhido usuarioId={usuario.id}>{children}</CanteiroEscolhido>
  );
}

const semEscolha = async () => undefined;

function CanteiroDoAlmoxarife({
  canteiroId,
  children,
}: {
  canteiroId: string;
  children: ReactNode;
}) {
  const consulta = useQuery({
    queryKey: chavesCanteiros.detalhe(canteiroId),
    queryFn: () => buscarCanteiro(canteiroId),
  });

  const valor = useMemo<ValorCanteiroAtivo>(
    () => ({
      status: 'pronto',
      canteiroId,
      canteiro: consulta.data,
      podeEscolher: false,
      escolher: semEscolha,
      tentarDeNovo: () => void consulta.refetch(),
    }),
    [canteiroId, consulta],
  );

  return <CanteiroAtivoContext.Provider value={valor}>{children}</CanteiroAtivoContext.Provider>;
}

function CanteiroEscolhido({ usuarioId, children }: { usuarioId: string; children: ReactNode }) {
  const queryClient = useQueryClient();
  const [guardado, setGuardado] = useState<string | null | undefined>(undefined);
  const ativos = useQuery({
    queryKey: chavesCanteiros.lista(true),
    queryFn: () => listarCanteiros(true),
  });

  useEffect(() => {
    let vivo = true;
    lerCanteiroAtivo(usuarioId)
      .then((id) => vivo && setGuardado(id))
      .catch(() => vivo && setGuardado(null));
    return () => {
      vivo = false;
    };
  }, [usuarioId]);

  const escolher = useCallback(
    async (canteiroId: string) => {
      await salvarCanteiroAtivo(usuarioId, canteiroId);
      setGuardado(canteiroId);
      await queryClient.invalidateQueries({
        predicate: (consulta) => consulta.queryKey[0] !== 'canteiros',
      });
    },
    [queryClient, usuarioId],
  );

  const canteiro = ativos.data?.find((item) => item.id === guardado);
  let status: StatusCanteiroAtivo;
  if (guardado === undefined || (ativos.isPending && guardado !== null)) {
    status = 'carregando';
  } else if (ativos.isError && !ativos.data) {
    status = 'erro';
  } else {
    status = canteiro ? 'pronto' : 'escolher';
  }

  const valor = useMemo<ValorCanteiroAtivo>(
    () => ({
      status,
      canteiroId: canteiro?.id ?? null,
      canteiro,
      podeEscolher: true,
      escolher,
      tentarDeNovo: () => void ativos.refetch(),
    }),
    [status, canteiro, escolher, ativos],
  );

  return <CanteiroAtivoContext.Provider value={valor}>{children}</CanteiroAtivoContext.Provider>;
}
