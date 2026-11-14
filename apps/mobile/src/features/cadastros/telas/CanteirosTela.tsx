import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { FAB, List } from 'react-native-paper';

import {
  AcessoNegado,
  EmptyState,
  ErrorState,
  FiltroSituacao,
  LoadingState,
  SeloSituacao,
  situacaoParaAtivo,
  TelaComCabecalho,
  type Situacao,
} from '@/shared';
import { paraAppError } from '@/shared/api';
import { chavesCanteiros, listarCanteiros } from '@/shared/canteiro';
import { SomentePerfil } from '@/shared/sessao';

export function CanteirosTela() {
  const router = useRouter();
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const ativo = situacaoParaAtivo(situacao);
  const consulta = useQuery({
    queryKey: chavesCanteiros.lista(ativo),
    queryFn: () => listarCanteiros(ativo),
  });

  return (
    <TelaComCabecalho titulo="Canteiros" aoVoltar={() => router.back()}>
      <SomentePerfil administrador alternativa={<AcessoNegado />}>
        <View style={styles.filtro}>
          <FiltroSituacao valor={situacao} aoMudar={setSituacao} />
        </View>
        {consulta.isPending ? (
          <LoadingState message="Carregando canteiros..." />
        ) : consulta.isError ? (
          <ErrorState
            message={paraAppError(consulta.error).message}
            onRetry={() => consulta.refetch()}
          />
        ) : (
          <FlatList
            data={consulta.data}
            keyExtractor={(canteiro) => canteiro.id}
            refreshControl={
              <RefreshControl
                refreshing={consulta.isRefetching}
                onRefresh={() => void consulta.refetch()}
              />
            }
            contentContainerStyle={consulta.data.length === 0 ? styles.vazio : styles.lista}
            ListEmptyComponent={
              <EmptyState
                title="Nenhum canteiro"
                description="Toque em Novo canteiro para cadastrar a primeira obra."
              />
            }
            renderItem={({ item }) => (
              <List.Item
                title={item.nome}
                description={item.endereco ?? 'Sem endereço'}
                left={(props) => <List.Icon {...props} icon="office-building-outline" />}
                right={() => <SeloSituacao ativo={item.ativo} />}
                onPress={() => router.push(`/cadastros/canteiros/${item.id}`)}
              />
            )}
          />
        )}
        <FAB
          icon="plus"
          label="Novo canteiro"
          style={styles.fab}
          onPress={() => router.push('/cadastros/canteiros/novo')}
        />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

const styles = StyleSheet.create({
  filtro: { padding: 16, paddingBottom: 8 },
  lista: { paddingBottom: 96 },
  vazio: { flexGrow: 1 },
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
