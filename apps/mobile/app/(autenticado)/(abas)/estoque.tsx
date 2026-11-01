import { useLocalSearchParams } from 'expo-router';

import { EstoqueTela } from '@/features/estoque';

export default function EstoqueRota() {
  const { filtro } = useLocalSearchParams<{ filtro?: string }>();
  return <EstoqueTela key={filtro ?? 'todos'} />;
}
