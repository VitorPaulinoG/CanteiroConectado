import { useLocalSearchParams } from 'expo-router';

import { NovaRequisicaoTela } from '@/features/requisicoes';

export default function NovaRequisicaoRota() {
  const { itemId } = useLocalSearchParams<{ itemId?: string }>();
  return <NovaRequisicaoTela itemIdInicial={itemId} />;
}
