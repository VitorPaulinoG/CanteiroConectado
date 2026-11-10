import { useLocalSearchParams } from 'expo-router';

import { FuncionarioFormTela } from '@/features/cadastros';

export default function FuncionarioRota() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <FuncionarioFormTela id={id === 'novo' ? undefined : id} />;
}
