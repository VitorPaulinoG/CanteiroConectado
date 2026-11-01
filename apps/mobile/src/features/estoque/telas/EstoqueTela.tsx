import { useInfiniteQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Chip, List, Searchbar } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, StatusBadge } from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { TelaDoCanteiro, useCanteiroIdAtivo } from '@/shared/canteiro';
import { chavesEstoque, listarEstoque, seloDoStatus } from '@/shared/estoque';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { useAtrasado } from '@/shared/utils/useAtrasado';

import { FILTROS_DE_STATUS, lerFiltro, statusDoFiltro, type FiltroDeStatus } from '../filtros';

export function EstoqueTela() {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const parametros = useLocalSearchParams<{ filtro?: string }>();
  const [filtroDeStatus, setFiltroDeStatus] = useState<FiltroDeStatus>(
    lerFiltro(parametros.filtro),
  );
  const [busca, setBusca] = useState('');
  const termo = useAtrasado(busca.trim());
  const filtro = { q: termo || undefined, status: statusDoFiltro(filtroDeStatus) };

  const consulta = useInfiniteQuery({
    queryKey: chavesEstoque.lista(canteiroId, filtro),
    queryFn: ({ pageParam }) => listarEstoque(canteiroId, filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const itens = juntarPaginas(consulta.data?.pages);

  return (
    <TelaDoCanteiro titulo="Estoque">
      <View style={styles.filtros}>
        <Searchbar
          placeholder="Buscar por código ou descrição"
          value={busca}
          onChangeText={setBusca}
          accessibilityLabel="Buscar no estoque"
        />
        <View style={styles.chips}>
          {FILTROS_DE_STATUS.map((opcao) => (
            <Chip
              key={opcao.valor}
              selected={filtroDeStatus === opcao.valor}
              showSelectedCheck
              onPress={() => setFiltroDeStatus(opcao.valor)}
            >
              {opcao.rotulo}
            </Chip>
          ))}
        </View>
      </View>
      {consulta.isPending ? (
        <LoadingState message="Carregando o estoque..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          testID="lista-do-estoque"
          data={itens}
          keyExtractor={(item) => item.itemId}
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
          contentContainerStyle={itens.length === 0 ? styles.vazio : undefined}
          ListEmptyComponent={
            <EmptyState
              title={
                filtroDeStatus === 'todos' ? 'Nenhum item no catálogo' : 'Nenhum item em alerta'
              }
              description={
                filtroDeStatus === 'todos'
                  ? 'Cadastre itens em Mais → Cadastros → Itens.'
                  : 'Tudo certo por aqui.'
              }
            />
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
          }
          renderItem={({ item }) => (
            <List.Item
              title={item.descricao}
              description={`${item.codigo} · ${formatarQuantidade(item.saldo, item.unidadeSimbolo)}`}
              right={() => (
                <View style={styles.selo}>
                  <StatusBadge status={seloDoStatus(item.status)} />
                </View>
              )}
              onPress={() => router.push(`/estoque/${item.itemId}`)}
            />
          )}
        />
      )}
    </TelaDoCanteiro>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 8, gap: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  selo: { justifyContent: 'center' },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
});
