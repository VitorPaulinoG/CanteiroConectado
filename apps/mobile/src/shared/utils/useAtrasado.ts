import { useEffect, useState } from 'react';

export const ATRASO_DE_BUSCA_MS = 300;

export function useAtrasado<T>(valor: T, atrasoMs = ATRASO_DE_BUSCA_MS): T {
  const [atrasado, setAtrasado] = useState(valor);

  useEffect(() => {
    const espera = setTimeout(() => setAtrasado(valor), atrasoMs);
    return () => clearTimeout(espera);
  }, [valor, atrasoMs]);

  return atrasado;
}
