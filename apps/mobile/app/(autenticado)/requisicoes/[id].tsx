import { useLocalSearchParams } from 'expo-router';

import { DetalheRequisicaoTela } from '@/features/requisicoes';

export default function DetalheRequisicaoRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <DetalheRequisicaoTela id={id} />;
}
