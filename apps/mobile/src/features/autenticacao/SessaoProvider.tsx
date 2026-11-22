import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';

import { definirAoPerderSessao, definirLeitorDeToken } from '@/shared/api';
import {
  AVISO_SESSAO_EXPIRADA,
  SessaoContext,
  type EstadoSessao,
  type UsuarioSessao,
  type ValorSessao,
} from '@/shared/sessao';

import { buscarEu, postarLogin } from './api';
import { apagarSessao, lerSessao, salvarSessao, sessaoExpirada, type SessaoSalva } from './storage';

export function SessaoProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState<EstadoSessao>({ status: 'carregando' });
  const sessaoRef = useRef<SessaoSalva | null>(null);

  const encerrar = useCallback(
    async (aviso?: string) => {
      sessaoRef.current = null;
      setEstado({ status: 'anonimo', aviso });
      queryClient.clear();
      await apagarSessao();
    },
    [queryClient],
  );

  useEffect(() => {
    definirLeitorDeToken(() => sessaoRef.current?.token ?? null);
    definirAoPerderSessao(() => {
      if (sessaoRef.current) {
        void encerrar(AVISO_SESSAO_EXPIRADA);
      }
    });
    return () => {
      definirLeitorDeToken(() => null);
      definirAoPerderSessao(() => undefined);
    };
  }, [encerrar]);

  useEffect(() => {
    let ativo = true;
    lerSessao()
      .then(async (salva) => {
        if (!ativo) {
          return;
        }
        if (!salva) {
          setEstado({ status: 'anonimo' });
          return;
        }
        if (sessaoExpirada(salva)) {
          await apagarSessao();
          setEstado({ status: 'anonimo', aviso: AVISO_SESSAO_EXPIRADA });
          return;
        }
        sessaoRef.current = salva;
        setEstado({ status: 'autenticado', usuario: salva.usuario, trocaAdiada: false });
        try {
          const usuario = await buscarEu();
          if (ativo && sessaoRef.current?.token === salva.token) {
            sessaoRef.current = { ...salva, usuario };
            setEstado((atual) => (atual.status === 'autenticado' ? { ...atual, usuario } : atual));
            await salvarSessao(sessaoRef.current);
          }
        } catch {
          return;
        }
      })
      .catch(() => ativo && setEstado({ status: 'anonimo' }));
    return () => {
      ativo = false;
    };
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const resposta = await postarLogin(email, senha);
    const sessao: SessaoSalva = {
      token: resposta.token,
      expiraEm: resposta.expiraEm,
      usuario: resposta.usuario,
    };
    sessaoRef.current = sessao;
    await salvarSessao(sessao);
    setEstado({ status: 'autenticado', usuario: resposta.usuario, trocaAdiada: false });
  }, []);

  const sair = useCallback(() => encerrar(), [encerrar]);

  const atualizarUsuario = useCallback((usuario: UsuarioSessao) => {
    if (!sessaoRef.current) {
      return;
    }
    sessaoRef.current = { ...sessaoRef.current, usuario };
    void salvarSessao(sessaoRef.current);
    setEstado((atual) => (atual.status === 'autenticado' ? { ...atual, usuario } : atual));
  }, []);

  const adiarTroca = useCallback(() => {
    setEstado((atual) =>
      atual.status === 'autenticado' ? { ...atual, trocaAdiada: true } : atual,
    );
  }, []);

  const valor = useMemo<ValorSessao>(
    () => ({ estado, entrar, sair, atualizarUsuario, adiarTroca }),
    [estado, entrar, sair, atualizarUsuario, adiarTroca],
  );

  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>;
}
