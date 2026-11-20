import { useInfiniteQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, FAB, SegmentedButtons } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState } from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { TelaDoCanteiro, useCanteiroIdAtivo } from '@/shared/canteiro';
import { useAtualizarAoFocar } from '@/shared/utils/useAtualizarAoFocar';

import { chavesRequisicoes, listarRequisicoes, type Escopo } from '../api';
import { CartaoDeRequisicao } from '../componentes/CartaoDeRequisicao';

function lerAba(valor: string | undefined): Escopo {
  return valor === 'para-decidir' ? 'para-decidir' : 'minhas';
}

export function RequisicoesTela() {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const parametros = useLocalSearchParams<{ aba?: string }>();
  const [escopo, setEscopo] = useState<Escopo>(lerAba(parametros.aba));

  const consulta = useInfiniteQuery({
    queryKey: chavesRequisicoes.lista(canteiroId, escopo),
    queryFn: ({ pageParam }) => listarRequisicoes(canteiroId, escopo, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  useAtualizarAoFocar(() => consulta.refetch());
  const requisicoes = juntarPaginas(consulta.data?.pages);

  return (
    <TelaDoCanteiro titulo="Requisições">
      <View style={styles.abas}>
        <SegmentedButtons
          value={escopo}
          onValueChange={(valor) => setEscopo(valor as Escopo)}
          buttons={[
            { value: 'minhas', label: 'Minhas', icon: 'account-outline' },
            { value: 'para-decidir', label: 'Para decidir', icon: 'gavel' },
          ]}
        />
      </View>
      {consulta.isPending ? (
        <LoadingState message="Carregando requisições..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          testID="lista-de-requisicoes"
          data={requisicoes}
          keyExtractor={(requisicao) => requisicao.id}
          onEndReached={() => {
            if (consulta.hasNextPage && !consulta.isFetchingNextPage) {
              void consulta.fetchNextPage();
            }
          }}
          onEndReachedThreshold={0.5}
          refreshControl={
            <RefreshControl
              refreshing={consulta.isRefetching && !consulta.isFetchingNextPage}
              onRefresh={() => void consulta.refetch()}
            />
          }
          contentContainerStyle={requisicoes.length === 0 ? styles.vazio : styles.lista}
          ListEmptyComponent={
            escopo === 'minhas' ? (
              <EmptyState
                title="Nenhuma requisição sua"
                description="Toque em Nova requisição para pedir material."
              />
            ) : (
              <EmptyState
                title="Nada para decidir"
                description="As requisições da outra pessoa aparecem aqui."
              />
            )
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
          }
          renderItem={({ item }) => (
            <CartaoDeRequisicao
              requisicao={item}
              mostrarSolicitante={escopo === 'para-decidir'}
              aoTocar={() => router.push(`/requisicoes/${item.id}`)}
            />
          )}
        />
      )}
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
  abas: { padding: 16, paddingBottom: 8 },
  lista: { paddingBottom: 96 },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
