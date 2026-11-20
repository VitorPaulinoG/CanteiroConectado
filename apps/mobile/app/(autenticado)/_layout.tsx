import { Stack } from 'expo-router';

import { ErrorState, LoadingState } from '@/shared';
import { CanteiroAtivoProvider, useCanteiroAtivo } from '@/shared/canteiro';

function AreaDoCanteiro() {
  const { status, tentarDeNovo } = useCanteiroAtivo();

  if (status === 'carregando') {
    return <LoadingState message="Carregando o canteiro..." />;
  }
  if (status === 'erro') {
    return <ErrorState message="Não foi possível carregar os canteiros." onRetry={tentarDeNovo} />;
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={status === 'pronto'} redirectTo="/canteiro">
        <Stack.Screen name="(abas)" />
        <Stack.Screen name="estoque/[id]" />
        <Stack.Screen name="movimentar/saida" />
        <Stack.Screen name="movimentar/entrada" />
      </Stack.Protected>
      <Stack.Screen name="canteiro" />
    </Stack>
  );
}

export default function AreaAutenticadaLayout() {
  return (
    <CanteiroAtivoProvider>
      <AreaDoCanteiro />
    </CanteiroAtivoProvider>
  );
}
