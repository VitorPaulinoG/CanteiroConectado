import { StyleSheet, View } from 'react-native';
import { ActivityIndicator, Avatar, Text } from 'react-native-paper';

import { useAppTheme } from '@/theme';

export function AberturaTela() {
  const theme = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Avatar.Icon icon="hard-hat" size={96} />
      <Text variant="headlineSmall">Canteiro Conectado</Text>
      <ActivityIndicator size="large" accessibilityLabel="Carregando" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 24 },
});
