import { BottomTabBarHeightContext } from 'expo-router/tabs';
import { useContext, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Appbar, Text } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

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
  const { bottom } = useSafeAreaInsets();
  const dentroDasAbas = useContext(BottomTabBarHeightContext) !== undefined;

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
      <View
        testID="conteudo-da-tela"
        style={[styles.tela, { marginBottom: dentroDasAbas ? 0 : bottom }]}
      >
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
});
