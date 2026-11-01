import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ambiente } from '@/config';
import { ConfiguracaoInvalida } from '@/shared';
import { criarQueryClient } from '@/shared/api';
import { AvisoServidor, ServidorProvider } from '@/shared/servidor';
import { appTheme } from '@/theme';

export default function RootLayout() {
  const [queryClient] = useState(criarQueryClient);

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <PaperProvider theme={appTheme}>
          {ambiente.valido ? (
            <ServidorProvider>
              <View style={styles.tela}>
                <AvisoServidor />
                <Stack>
                  <Stack.Screen name="index" options={{ headerShown: false }} />
                  <Stack.Screen name="sobre" options={{ title: 'Sobre' }} />
                  <Stack.Protected guard={__DEV__}>
                    <Stack.Screen name="playground" options={{ title: 'Playground' }} />
                  </Stack.Protected>
                </Stack>
              </View>
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
