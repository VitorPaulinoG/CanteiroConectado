import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

type Props = { message?: string; onRetry: () => void };

export function ErrorState({ message = 'Algo deu errado.', onRetry }: Props) {
  return (
    <View style={styles.container}>
      <Text variant="titleMedium">{message}</Text>
      <Button mode="contained" onPress={onRetry}>
        Tentar de novo
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16, padding: 24 },
});
