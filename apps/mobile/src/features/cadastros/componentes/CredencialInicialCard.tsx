import { useQuery } from '@tanstack/react-query';
import * as Clipboard from 'expo-clipboard';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { ActivityIndicator, Card, IconButton, Snackbar, Text } from 'react-native-paper';

import { paraAppError } from '@/shared/api';

import { buscarCredencialInicial, chavesFuncionarios } from '../usuarios';

type Props = { funcionarioId: string };

export function CredencialInicialCard({ funcionarioId }: Props) {
  const [copiado, setCopiado] = useState(false);
  const credencial = useQuery({
    queryKey: chavesFuncionarios.credencial(funcionarioId),
    queryFn: () => buscarCredencialInicial(funcionarioId),
    staleTime: 0,
    gcTime: 0,
  });

  if (credencial.isError && paraAppError(credencial.error).codigo === 'CREDENCIAL_JA_TROCADA') {
    return null;
  }

  async function copiar() {
    if (!credencial.data) {
      return;
    }
    await Clipboard.setStringAsync(
      `E-mail: ${credencial.data.email}\nSenha temporária: ${credencial.data.senhaTemporaria}`,
    );
    setCopiado(true);
  }

  return (
    <Card mode="outlined" style={styles.cartao}>
      <Card.Title
        title="Credencial inicial"
        subtitle="Entregue ao funcionário. Some quando ele trocar a senha."
        subtitleNumberOfLines={2}
        right={(props) =>
          credencial.data ? (
            <IconButton
              {...props}
              icon="content-copy"
              accessibilityLabel="Copiar credencial"
              onPress={() => void copiar()}
            />
          ) : null
        }
      />
      <Card.Content>
        {credencial.isPending ? (
          <ActivityIndicator accessibilityLabel="Carregando credencial" />
        ) : credencial.isError ? (
          <Text variant="bodyMedium">{paraAppError(credencial.error).message}</Text>
        ) : (
          <View style={styles.linhas}>
            <Text variant="bodyLarge">E-mail: {credencial.data.email}</Text>
            <Text variant="titleMedium" selectable>
              Senha temporária: {credencial.data.senhaTemporaria}
            </Text>
          </View>
        )}
      </Card.Content>
      <Snackbar visible={copiado} onDismiss={() => setCopiado(false)} duration={2000}>
        Credencial copiada.
      </Snackbar>
    </Card>
  );
}

const styles = StyleSheet.create({
  cartao: { marginBottom: 16 },
  linhas: { gap: 4 },
});
