// Rota de desenvolvimento: protegida por `__DEV__` no _layout.tsx.
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, Divider, Text, TextInput } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, QuantityStepper, StatusBadge } from '@/shared';
import { useAppTheme } from '@/theme';

export default function Playground() {
  const theme = useAppTheme();
  const [quantity, setQuantity] = useState(5);
  const [text, setText] = useState('');

  return (
    <ScrollView
      style={{ backgroundColor: theme.colors.background }}
      contentContainerStyle={styles.content}
    >
      <Text variant="titleLarge">Paper</Text>
      <Button mode="contained">Botão primário</Button>
      <Button mode="outlined">Botão secundário</Button>
      <TextInput label="Campo de texto" value={text} onChangeText={setText} />
      <Card>
        <Card.Title title="Cartão" subtitle="Exemplo do Paper" />
        <Card.Content>
          <Text variant="bodyMedium">Conteúdo do cartão</Text>
        </Card.Content>
      </Card>
      <Divider />

      <Text variant="titleLarge">StatusBadge</Text>
      <View style={styles.row}>
        <StatusBadge status="normal" />
        <StatusBadge status="belowMinimum" />
        <StatusBadge status="empty" />
      </View>

      <Text variant="titleLarge">QuantityStepper (0–10)</Text>
      <QuantityStepper value={quantity} onChange={setQuantity} min={0} max={10} />

      <Text variant="titleLarge">LoadingState</Text>
      <View style={styles.box}>
        <LoadingState />
      </View>
      <Text variant="titleLarge">EmptyState</Text>
      <View style={styles.box}>
        <EmptyState title="Nada por aqui" description="Nenhum item encontrado." />
      </View>
      <Text variant="titleLarge">ErrorState</Text>
      <View style={styles.box}>
        <ErrorState onRetry={() => undefined} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 16, gap: 12 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  box: { height: 160 },
});
