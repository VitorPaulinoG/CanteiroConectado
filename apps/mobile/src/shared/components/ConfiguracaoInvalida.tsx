import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/theme';

type Props = { problema: string };

export function ConfiguracaoInvalida({ problema }: Props) {
  const theme = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Text variant="headlineSmall" accessibilityRole="header">
        Configuração incompleta
      </Text>
      <Text variant="bodyLarge">{problema}</Text>
      <Text variant="bodyMedium">
        Defina a variável no arquivo apps/mobile/.env (por exemplo
        EXPO_PUBLIC_API_URL=http://10.0.2.2:8080) e reinicie o Expo com npx expo start -c.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', gap: 16, padding: 24 },
});
