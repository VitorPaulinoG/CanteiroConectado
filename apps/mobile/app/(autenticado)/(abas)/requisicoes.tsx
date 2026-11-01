import { useRouter } from 'expo-router';
import { StyleSheet } from 'react-native';
import { FAB } from 'react-native-paper';

import { EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';

export default function Requisicoes() {
  const router = useRouter();
  return (
    <TelaDoCanteiro titulo="Requisições">
      <EmptyState title="Requisições" description="Suas requisições e as que esperam decisão." />
      <FAB
        icon="clipboard-plus-outline"
        label="Nova requisição"
        style={styles.fab}
        onPress={() => router.push('/requisicoes/nova')}
      />
    </TelaDoCanteiro>
  );
}

const styles = StyleSheet.create({
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
