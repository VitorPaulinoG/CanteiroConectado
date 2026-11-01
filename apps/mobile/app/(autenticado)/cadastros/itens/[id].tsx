import { useLocalSearchParams } from 'expo-router';

import { ItemFormTela } from '@/features/cadastros';

export default function ItemRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ItemFormTela id={id === 'novo' ? undefined : id} />;
}
