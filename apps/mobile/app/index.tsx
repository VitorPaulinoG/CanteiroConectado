import { StyleSheet, View } from 'react-native';
import { Link } from 'expo-router';
import { Button, Text } from 'react-native-paper';

import { useAppTheme } from '@/theme';

export default function TelaInicial() {
  const theme = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Text variant="headlineSmall">Canteiro Conectado</Text>
      <Link href="/sobre" asChild>
        <Button mode="text">Sobre</Button>
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
