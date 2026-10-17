import { Stack } from 'expo-router';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { appTheme } from '@/theme';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <PaperProvider theme={appTheme}>
        <Stack>
          <Stack.Screen name="index" options={{ headerShown: false }} />
          <Stack.Protected guard={__DEV__}>
            <Stack.Screen name="playground" options={{ title: 'Playground' }} />
          </Stack.Protected>
        </Stack>
      </PaperProvider>
    </SafeAreaProvider>
  );
}
