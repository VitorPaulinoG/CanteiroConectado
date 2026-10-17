import { StyleSheet, View } from 'react-native';
import { IconButton, Text } from 'react-native-paper';

type Props = {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
};

export function QuantityStepper({
  value,
  onChange,
  min = 0,
  max = Number.MAX_SAFE_INTEGER,
}: Props) {
  return (
    <View style={styles.container}>
      <IconButton
        icon="minus"
        mode="contained-tonal"
        accessibilityLabel="Diminuir quantidade"
        disabled={value <= min}
        onPress={() => onChange(Math.max(min, value - 1))}
      />
      <Text variant="titleMedium" style={styles.value} accessibilityLiveRegion="polite">
        {value}
      </Text>
      <IconButton
        icon="plus"
        mode="contained-tonal"
        accessibilityLabel="Aumentar quantidade"
        disabled={value >= max}
        onPress={() => onChange(Math.min(max, value + 1))}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flexDirection: 'row', alignItems: 'center' },
  value: { minWidth: 40, textAlign: 'center' },
});
