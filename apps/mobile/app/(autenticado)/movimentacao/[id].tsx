import { useLocalSearchParams } from 'expo-router';

import { DetalheMovimentacaoTela } from '@/features/movimentacoes';

export default function DetalheMovimentacaoRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <DetalheMovimentacaoTela key={id} id={id} />;
}
