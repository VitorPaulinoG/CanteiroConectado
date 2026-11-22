import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';

import { buscarPing } from '@/shared/api/ping';

import { acordarServidor, AVISO_APOS_MS } from './acordarServidor';

export type EstadoServidor = 'verificando' | 'acordando' | 'pronto' | 'falhou';

type ValorServidor = {
  estado: EstadoServidor;
  tentarDeNovo: () => void;
};

const ServidorContext = createContext<ValorServidor>({
  estado: 'pronto',
  tentarDeNovo: () => undefined,
});

type Props = {
  children: ReactNode;
  chamar?: () => Promise<unknown>;
};

export function ServidorProvider({ children, chamar = buscarPing }: Props) {
  const [estado, setEstado] = useState<EstadoServidor>('verificando');
  const [tentativa, setTentativa] = useState(0);
  const chamarRef = useRef(chamar);

  useEffect(() => {
    let ativo = true;
    const aviso = setTimeout(() => {
      if (ativo) {
        setEstado('acordando');
      }
    }, AVISO_APOS_MS);

    acordarServidor({ chamar: () => chamarRef.current() })
      .then(() => ativo && setEstado('pronto'))
      .catch(() => ativo && setEstado('falhou'))
      .finally(() => clearTimeout(aviso));

    return () => {
      ativo = false;
      clearTimeout(aviso);
    };
  }, [tentativa]);

  const tentarDeNovo = useCallback(() => {
    setEstado('verificando');
    setTentativa((n) => n + 1);
  }, []);
  const valor = useMemo(() => ({ estado, tentarDeNovo }), [estado, tentarDeNovo]);

  return <ServidorContext.Provider value={valor}>{children}</ServidorContext.Provider>;
}

export function useServidor() {
  return useContext(ServidorContext);
}
