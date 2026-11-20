import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef } from 'react';

export function useAtualizarAoFocar(atualizar: () => unknown) {
  const primeiraVez = useRef(true);
  const ultima = useRef(atualizar);

  useEffect(() => {
    ultima.current = atualizar;
  });

  useFocusEffect(
    useCallback(() => {
      if (primeiraVez.current) {
        primeiraVez.current = false;
        return;
      }
      void ultima.current();
    }, []),
  );
}
