import { useLocalSearchParams } from 'expo-router';

import { SaidaTela } from '@/features/movimentacoes';

export default function SaidaRota() {
  const { itemId } = useLocalSearchParams<{ itemId?: string }>();
  return <SaidaTela itemIdInicial={itemId} />;
}
