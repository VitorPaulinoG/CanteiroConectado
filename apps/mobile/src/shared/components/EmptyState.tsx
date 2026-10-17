import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

type Props = { title: string; description?: string };

export function EmptyState({ title, description }: Props) {
  return (
    <View style={styles.container}>
      <Text variant="titleMedium">{title}</Text>
      {description ? <Text variant="bodyMedium">{description}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 8, padding: 24 },
});
