import { StyleSheet, View } from 'react-native';
import { ActivityIndicator, Banner, Chip, Text } from 'react-native-paper';

import type { AppError, EstadoDeEnvio } from '@/shared/api';
import { useAppTheme } from '@/theme';

type Props = {
  estado: EstadoDeEnvio;
  erro?: AppError | null;
  aoTentarDeNovo?: () => void;
  aoCorrigir?: () => void;
};

export function FaixaEstadoEnvio({ estado, erro, aoTentarDeNovo, aoCorrigir }: Props) {
  const theme = useAppTheme();

  if (estado === 'enviando') {
    return (
      <View
        style={[styles.enviando, { backgroundColor: theme.colors.secondaryContainer }]}
        accessibilityRole="progressbar"
        accessibilityLiveRegion="polite"
      >
        <ActivityIndicator size="small" />
        <Text variant="titleMedium" style={{ color: theme.colors.onSecondaryContainer }}>
          Enviando…
        </Text>
      </View>
    );
  }

  if (estado === 'salvo') {
    return (
      <View style={styles.salvo} accessibilityLiveRegion="polite">
        <Chip
          icon="check-circle"
          style={{ backgroundColor: theme.status.normalContainer }}
          textStyle={{ color: theme.status.onNormalContainer }}
        >
          Salvo no servidor
        </Chip>
      </View>
    );
  }

  if (estado === 'falhou') {
    const semResposta = erro?.semResposta ?? true;
    const acoes = semResposta
      ? aoTentarDeNovo
        ? [{ label: 'Tentar de novo', onPress: aoTentarDeNovo }]
        : []
      : aoCorrigir
        ? [{ label: 'Corrigir', onPress: aoCorrigir }]
        : [];
    return (
      <Banner
        visible
        icon={semResposta ? 'cloud-off-outline' : 'alert-circle-outline'}
        actions={acoes}
      >
        {semResposta
          ? `Não foi possível enviar. ${erro?.message ?? 'Confira a internet e tente de novo.'}`
          : (erro?.message ?? 'O servidor recusou o registro.')}
      </Banner>
    );
  }

  return null;
}

const styles = StyleSheet.create({
  enviando: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 12 },
  salvo: { flexDirection: 'row' },
});
