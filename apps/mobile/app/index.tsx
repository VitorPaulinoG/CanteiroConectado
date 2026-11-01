import { Redirect } from 'expo-router';

import { AberturaTela } from '@/features/autenticacao';
import { useSessao } from '@/shared/sessao';

export default function Abertura() {
  const { estado } = useSessao();

  if (estado.status === 'carregando') {
    return <AberturaTela />;
  }
  if (estado.status === 'anonimo') {
    return <Redirect href="/login" />;
  }
  return <Redirect href="/inicio" />;
}
