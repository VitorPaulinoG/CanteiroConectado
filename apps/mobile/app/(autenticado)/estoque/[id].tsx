import { useLocalSearchParams } from 'expo-router';

import { DetalheItemTela } from '@/features/estoque';

export default function DetalheItemRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <DetalheItemTela itemId={id} />;
}
