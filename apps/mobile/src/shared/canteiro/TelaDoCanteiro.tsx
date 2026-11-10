import { useRouter } from 'expo-router';
import type { ComponentProps } from 'react';

import { TelaComCabecalho } from '@/shared/components/TelaComCabecalho';

import { useCanteiroAtivo } from './CanteiroAtivoContext';

type Props = Omit<
  ComponentProps<typeof TelaComCabecalho>,
  'subtitulo' | 'aoTocarTitulo' | 'rotuloDoTitulo'
>;

export function TelaDoCanteiro(props: Props) {
  const router = useRouter();
  const { canteiro, podeEscolher } = useCanteiroAtivo();
  const nome = canteiro?.nome ?? '';

  return (
    <TelaComCabecalho
      {...props}
      subtitulo={podeEscolher ? `${nome} ▾` : nome}
      aoTocarTitulo={podeEscolher ? () => router.push('/canteiro') : undefined}
      rotuloDoTitulo={
        podeEscolher ? `${props.titulo}. Canteiro ${nome}. Trocar canteiro` : undefined
      }
    />
  );
}
