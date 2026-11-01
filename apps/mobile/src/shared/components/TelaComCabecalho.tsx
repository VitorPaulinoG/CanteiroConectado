import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Appbar } from 'react-native-paper';

import { useAppTheme } from '@/theme';

type Props = {
  titulo: string;
  subtitulo?: string;
  aoVoltar?: () => void;
  acoes?: ReactNode;
  children: ReactNode;
};

export function TelaComCabecalho({ titulo, subtitulo, aoVoltar, acoes, children }: Props) {
  const theme = useAppTheme();

  return (
    <View style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <Appbar.Header elevated>
        {aoVoltar ? <Appbar.BackAction onPress={aoVoltar} accessibilityLabel="Voltar" /> : null}
        <Appbar.Content title={titulo} subtitle={subtitulo} />
        {acoes}
      </Appbar.Header>
      <View style={styles.tela}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
});
