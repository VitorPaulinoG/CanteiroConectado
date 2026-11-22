import { useQuery } from '@tanstack/react-query';
import { ScrollView, StyleSheet } from 'react-native';
import { List } from 'react-native-paper';

import { buscarPing, paraAppError } from '@/shared/api';
import { ErrorState, LoadingState } from '@/shared';
import { versaoDoApp } from '@/shared/utils/appVersion';
import { formatarDataHora } from '@/shared/utils/format';
import { useAppTheme } from '@/theme';

export function SobreTela() {
  const theme = useAppTheme();
  const ping = useQuery({ queryKey: ['ping'], queryFn: () => buscarPing(), staleTime: 0 });

  return (
    <ScrollView
      style={{ backgroundColor: theme.colors.background }}
      contentContainerStyle={styles.conteudo}
    >
      <List.Section>
        <List.Subheader>Aplicativo</List.Subheader>
        <List.Item
          title="Versão do app"
          description={versaoDoApp()}
          left={(p) => <List.Icon {...p} icon="cellphone" />}
        />
      </List.Section>
      <List.Section>
        <List.Subheader>Servidor</List.Subheader>
        {ping.isPending ? (
          <LoadingState message="Consultando o servidor..." />
        ) : ping.isError ? (
          <ErrorState message={paraAppError(ping.error).message} onRetry={() => ping.refetch()} />
        ) : (
          <>
            <List.Item
              title="API"
              description={ping.data.nome}
              left={(p) => <List.Icon {...p} icon="server" />}
            />
            <List.Item
              title="Versão da API"
              description={ping.data.versao}
              left={(p) => <List.Icon {...p} icon="tag-outline" />}
            />
            <List.Item
              title="Hora do servidor"
              description={formatarDataHora(ping.data.horaServidor)}
              left={(p) => <List.Icon {...p} icon="clock-outline" />}
            />
          </>
        )}
      </List.Section>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  conteudo: { paddingVertical: 8 },
});
