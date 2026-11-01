import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ambiente } from '@/config';
import { SessaoProvider } from '@/features/autenticacao';
import { ConfiguracaoInvalida } from '@/shared';
import { criarQueryClient } from '@/shared/api';
import { AvisoServidor, ServidorProvider } from '@/shared/servidor';
import { useSessao } from '@/shared/sessao';
import { appTheme } from '@/theme';

export function Navegacao() {
  const { estado } = useSessao();
  const anonimo = estado.status === 'anonimo';
  const autenticado = estado.status === 'autenticado';

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Protected guard={anonimo}>
        <Stack.Screen name="(autenticacao)/login" />
      </Stack.Protected>
      <Stack.Protected guard={autenticado}>
        <Stack.Screen name="(autenticado)" />
      </Stack.Protected>
      <Stack.Screen name="sobre" options={{ headerShown: true, title: 'Sobre' }} />
      <Stack.Protected guard={__DEV__}>
        <Stack.Screen name="playground" options={{ headerShown: true, title: 'Playground' }} />
      </Stack.Protected>
    </Stack>
  );
}

export default function RootLayout() {
  const [queryClient] = useState(criarQueryClient);

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <PaperProvider theme={appTheme}>
          {ambiente.valido ? (
            <ServidorProvider>
              <SessaoProvider>
                <View style={styles.tela}>
                  <AvisoServidor />
                  <Navegacao />
                </View>
              </SessaoProvider>
            </ServidorProvider>
          ) : (
            <ConfiguracaoInvalida problema={ambiente.problema} />
          )}
        </PaperProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
});
