import { useInfiniteQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, FAB, List, Searchbar } from 'react-native-paper';

import {
  AcessoNegado,
  EmptyState,
  ErrorState,
  FiltroSituacao,
  LoadingState,
  SeloSituacao,
  situacaoParaAtivo,
  type Situacao,
} from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { TelaDoCanteiro, useCanteiroIdAtivo } from '@/shared/canteiro';
import {
  ColaboradorModal,
  chavesColaboradores,
  listarColaboradores,
  type Colaborador,
} from '@/shared/colaborador';
import { SomentePerfil } from '@/shared/sessao';
import { useAtrasado } from '@/shared/utils/useAtrasado';

export function ColaboradoresTela() {
  const router = useRouter();
  return (
    <TelaDoCanteiro titulo="Colaboradores" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE', 'GESTOR']} alternativa={<AcessoNegado />}>
        <Lista />
      </SomentePerfil>
    </TelaDoCanteiro>
  );
}

function Lista() {
  const canteiroId = useCanteiroIdAtivo();
  const [busca, setBusca] = useState('');
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const [editando, setEditando] = useState<Colaborador | 'novo' | null>(null);
  const termo = useAtrasado(busca.trim());
  const filtro = { q: termo || undefined, ativo: situacaoParaAtivo(situacao) };
  const consulta = useInfiniteQuery({
    queryKey: chavesColaboradores.lista(canteiroId, filtro),
    queryFn: ({ pageParam }) => listarColaboradores(canteiroId, filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const colaboradores = juntarPaginas(consulta.data?.pages);

  return (
    <>
      <View style={styles.filtros}>
        <Searchbar
          placeholder="Buscar pelo nome"
          value={busca}
          onChangeText={setBusca}
          accessibilityLabel="Buscar colaborador"
        />
        <FiltroSituacao valor={situacao} aoMudar={setSituacao} />
      </View>
      {consulta.isPending ? (
        <LoadingState message="Carregando colaboradores..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          data={colaboradores}
          keyExtractor={(colaborador) => colaborador.id}
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
          contentContainerStyle={colaboradores.length === 0 ? styles.vazio : styles.lista}
          ListEmptyComponent={
            <EmptyState
              title="Nenhum colaborador"
              description="Cadastre quem retira material no canteiro."
            />
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
          }
          renderItem={({ item }) => (
            <List.Item
              title={item.nome}
              left={(props) => <List.Icon {...props} icon="account-outline" />}
              right={() => <SeloSituacao ativo={item.ativo} />}
              onPress={() => setEditando(item)}
            />
          )}
        />
      )}
      <FAB
        icon="account-plus-outline"
        label="Novo colaborador"
        style={styles.fab}
        onPress={() => setEditando('novo')}
      />
      <ColaboradorModal
        visivel={editando !== null}
        canteiroId={canteiroId}
        colaborador={editando === 'novo' || editando === null ? undefined : editando}
        aoSalvar={() => setEditando(null)}
        aoFechar={() => setEditando(null)}
      />
    </>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 8, gap: 12 },
  lista: { paddingBottom: 96 },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
