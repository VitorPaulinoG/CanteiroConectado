import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';
import { Avatar, Card, Text } from 'react-native-paper';

import { AcessoNegado } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';
import { SomentePerfil } from '@/shared/sessao';

export function MovimentarTela() {
  const router = useRouter();

  return (
    <TelaDoCanteiro titulo="Movimentar">
      <SomentePerfil perfis={['ALMOXARIFE']} alternativa={<AcessoNegado />}>
        <ScrollView contentContainerStyle={styles.conteudo}>
          <Card
            mode="elevated"
            onPress={() => router.push('/movimentar/saida')}
            accessibilityLabel="Registrar saída"
            accessibilityHint="Retirada de material ou ferramenta, ou ajuste por perda"
          >
            <Card.Title
              title="Saída"
              titleVariant="headlineSmall"
              subtitle="Retirada por colaborador ou ajuste"
              left={(props) => <Avatar.Icon {...props} icon="arrow-up-bold" />}
            />
            <Card.Content>
              <Text variant="bodyLarge">Quem levou, quanto e para qual frente de obra.</Text>
            </Card.Content>
          </Card>
          <Card
            mode="elevated"
            onPress={() => router.push('/movimentar/entrada')}
            accessibilityLabel="Registrar entrada"
            accessibilityHint="Recebimento de material, devolução de ferramenta ou ajuste"
          >
            <Card.Title
              title="Entrada"
              titleVariant="headlineSmall"
              subtitle="Recebimento, devolução ou ajuste"
              left={(props) => <Avatar.Icon {...props} icon="arrow-down-bold" />}
            />
            <Card.Content>
              <Text variant="bodyLarge">Material que chegou ou ferramenta que voltou.</Text>
            </Card.Content>
          </Card>
        </ScrollView>
      </SomentePerfil>
    </TelaDoCanteiro>
  );
}

const styles = StyleSheet.create({
  conteudo: { padding: 16, gap: 16 },
});
