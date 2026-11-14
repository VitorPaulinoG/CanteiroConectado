import { useLocalSearchParams } from 'expo-router';

import { CanteiroFormTela } from '@/features/cadastros';

export default function CanteiroRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <CanteiroFormTela id={id === 'novo' ? undefined : id} />;
}
