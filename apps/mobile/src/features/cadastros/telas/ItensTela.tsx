import { useInfiniteQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Chip, FAB, List, Searchbar } from 'react-native-paper';

import {
  AcessoNegado,
  EmptyState,
  ErrorState,
  FiltroSituacao,
  LoadingState,
  situacaoParaAtivo,
  TelaComCabecalho,
  type Situacao,
} from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { chavesItens, listarItens, ROTULO_DO_TIPO, type Item, type TipoItem } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { useAtrasado } from '@/shared/utils/useAtrasado';

const FILTROS_TIPO: { valor: TipoItem | undefined; rotulo: string }[] = [
  { valor: undefined, rotulo: 'Todos' },
  { valor: 'MATERIAL', rotulo: 'Materiais' },
  { valor: 'FERRAMENTA', rotulo: 'Ferramentas' },
];

function descricaoDaLinha(item: Item): string {
  return `${item.codigo} · ${ROTULO_DO_TIPO[item.tipo]} · ${item.unidadeSimbolo}`;
}

export function ItensTela() {
  const router = useRouter();
  const [busca, setBusca] = useState('');
  const [tipo, setTipo] = useState<TipoItem | undefined>(undefined);
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const termo = useAtrasado(busca.trim());
  const filtro = { q: termo || undefined, tipo, ativo: situacaoParaAtivo(situacao) };

  const consulta = useInfiniteQuery({
    queryKey: chavesItens.lista(filtro),
    queryFn: ({ pageParam }) => listarItens(filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const itens = juntarPaginas(consulta.data?.pages);

  return (
    <TelaComCabecalho titulo="Itens" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE', 'GESTOR']} alternativa={<AcessoNegado />}>
        <View style={styles.filtros}>
          <Searchbar
            placeholder="Buscar por código ou descrição"
            value={busca}
            onChangeText={setBusca}
            accessibilityLabel="Buscar item"
          />
          <View style={styles.chips}>
            {FILTROS_TIPO.map((opcao) => (
              <Chip
                key={opcao.rotulo}
                selected={tipo === opcao.valor}
                showSelectedCheck
                onPress={() => setTipo(opcao.valor)}
              >
                {opcao.rotulo}
              </Chip>
            ))}
          </View>
          <FiltroSituacao valor={situacao} aoMudar={setSituacao} />
        </View>
        {consulta.isPending ? (
          <LoadingState message="Carregando itens..." />
        ) : consulta.isError ? (
          <ErrorState
            message={paraAppError(consulta.error).message}
            onRetry={() => consulta.refetch()}
          />
        ) : (
          <FlatList
            testID="lista-de-itens"
            data={itens}
            keyExtractor={(item) => item.id}
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
            contentContainerStyle={itens.length === 0 ? styles.vazio : styles.lista}
            ListEmptyComponent={
              <EmptyState
                title="Nenhum item"
                description="Ajuste a busca ou os filtros, ou cadastre um item."
              />
            }
            ListFooterComponent={
              consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
            }
            renderItem={({ item }) => (
              <List.Item
                title={item.descricao}
                description={descricaoDaLinha(item)}
                left={(props) => (
                  <List.Icon
                    {...props}
                    icon={item.tipo === 'FERRAMENTA' ? 'hammer-wrench' : 'package-variant'}
                  />
                )}
                right={() =>
                  item.ativo ? null : (
                    <Chip compact icon="pause-circle-outline" style={styles.selo}>
                      Inativo
                    </Chip>
                  )
                }
                onPress={() => router.push(`/cadastros/itens/${item.id}`)}
              />
            )}
          />
        )}
        <FAB
          icon="plus"
          label="Novo item"
          style={styles.fab}
          onPress={() => router.push('/cadastros/itens/novo')}
        />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 8, gap: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  selo: { alignSelf: 'center' },
  lista: { paddingBottom: 96 },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
