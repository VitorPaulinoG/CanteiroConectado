import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Appbar, Text } from 'react-native-paper';

import { useAppTheme } from '@/theme';

type Props = {
  titulo: string;
  subtitulo?: string;
  aoTocarTitulo?: () => void;
  rotuloDoTitulo?: string;
  aoVoltar?: () => void;
  acoes?: ReactNode;
  children: ReactNode;
};

export function TelaComCabecalho({
  titulo,
  subtitulo,
  aoTocarTitulo,
  rotuloDoTitulo,
  aoVoltar,
  acoes,
  children,
}: Props) {
  const theme = useAppTheme();

  return (
    <View style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <Appbar.Header elevated>
        {aoVoltar ? <Appbar.BackAction onPress={aoVoltar} accessibilityLabel="Voltar" /> : null}
        <Appbar.Content
          title={
            subtitulo ? (
              <View>
                <Text variant="titleLarge" numberOfLines={1}>
                  {titulo}
                </Text>
                <Text
                  variant="bodyMedium"
                  numberOfLines={1}
                  style={{ color: theme.colors.onSurfaceVariant }}
                >
                  {subtitulo}
                </Text>
              </View>
            ) : (
              titulo
            )
          }
          onPress={aoTocarTitulo}
          accessibilityLabel={rotuloDoTitulo}
          accessibilityRole={aoTocarTitulo ? 'button' : undefined}
        />
        {acoes}
      </Appbar.Header>
      <View style={styles.tela}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
});
