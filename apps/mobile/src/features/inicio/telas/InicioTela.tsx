import { Link } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { useSessao, useUsuario } from '@/shared/sessao';
import { useAppTheme } from '@/theme';

export function InicioTela() {
  const theme = useAppTheme();
  const usuario = useUsuario();
  const { sair } = useSessao();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Text variant="headlineSmall">Olá, {usuario.nome}</Text>
      <Link href="/sobre" asChild>
        <Button mode="text">Sobre</Button>
      </Link>
      <Button mode="outlined" onPress={() => void sair()}>
        Sair
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16, padding: 24 },
});
