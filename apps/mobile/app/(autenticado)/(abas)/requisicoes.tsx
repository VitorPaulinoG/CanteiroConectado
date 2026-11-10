import { useLocalSearchParams } from 'expo-router';

import { RequisicoesTela } from '@/features/requisicoes';

export default function RequisicoesRota() {
  const { aba } = useLocalSearchParams<{ aba?: string }>();
  return <RequisicoesTela key={aba ?? 'minhas'} />;
}
