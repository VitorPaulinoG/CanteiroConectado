import { StyleSheet, View } from 'react-native';
import { Avatar, Text } from 'react-native-paper';

export function AcessoNegado() {
  return (
    <View style={styles.container} accessibilityRole="alert">
      <Avatar.Icon icon="lock-outline" size={72} />
      <Text variant="titleMedium">Você não tem acesso a esta área</Text>
      <Text variant="bodyMedium" style={styles.texto}>
        Esta parte do aplicativo é de outro perfil. Se precisar dela, fale com o administrador.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  texto: { textAlign: 'center' },
});
