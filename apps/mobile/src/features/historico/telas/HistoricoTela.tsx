import { useInfiniteQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { RefreshControl, SectionList, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Chip, List } from 'react-native-paper';

import { AcessoNegado, EmptyState, ErrorState, LoadingState } from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { TelaDoCanteiro, useCanteiroIdAtivo } from '@/shared/canteiro';
import { LinhaDeMovimentacao, type Movimentacao } from '@/shared/estoque';
import { SomentePerfil } from '@/shared/sessao';
import { useAtualizarAoFocar } from '@/shared/utils/useAtualizarAoFocar';

import { chavesHistorico, listarHistorico } from '../api';
import { FiltrosModal } from '../componentes/FiltrosModal';
import {
  paraParametros,
  quantosFiltrosExtras,
  rotuloDoDia,
  temFiltros,
  type Periodo,
} from '../filtros';
import { useFiltrosDoHistorico } from '../useFiltrosDoHistorico';

const PERIODOS: { valor: Periodo; rotulo: string }[] = [
  { valor: 'tudo', rotulo: 'Tudo' },
  { valor: 'hoje', rotulo: 'Hoje' },
  { valor: '7dias', rotulo: '7 dias' },
  { valor: 'personalizado', rotulo: 'Personalizado' },
];

export function agruparPorDia(movimentacoes: Movimentacao[], agora: Date = new Date()) {
  const secoes: { title: string; data: Movimentacao[] }[] = [];
  for (const movimentacao of movimentacoes) {
    const titulo = rotuloDoDia(movimentacao.criadoEm, agora);
    const ultima = secoes[secoes.length - 1];
    if (ultima && ultima.title === titulo) {
      ultima.data.push(movimentacao);
    } else {
      secoes.push({ title: titulo, data: [movimentacao] });
    }
  }
  return secoes;
}

export function HistoricoTela() {
  return (
    <TelaDoCanteiro titulo="Histórico">
      <SomentePerfil perfis={['GESTOR']} alternativa={<AcessoNegado />}>
        <Conteudo />
      </SomentePerfil>
    </TelaDoCanteiro>
  );
}

function Conteudo() {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const { filtros, mudar, limpar } = useFiltrosDoHistorico();
  const [abrindoFiltros, setAbrindoFiltros] = useState(false);
  const parametros = useMemo(() => paraParametros(filtros), [filtros]);

  const consulta = useInfiniteQuery({
    queryKey: chavesHistorico.lista(canteiroId, parametros),
    queryFn: ({ pageParam }) => listarHistorico(canteiroId, parametros, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  useAtualizarAoFocar(() => consulta.refetch());
  const secoes = agruparPorDia(juntarPaginas(consulta.data?.pages));
  const extras = quantosFiltrosExtras(filtros);

  function escolherPeriodo(periodo: Periodo) {
    mudar({ periodo });
    if (periodo === 'personalizado') {
      setAbrindoFiltros(true);
    }
  }

  return (
    <>
      <View style={styles.filtros}>
        <View style={styles.chips}>
          {PERIODOS.map((opcao) => (
            <Chip
              key={opcao.valor}
              selected={filtros.periodo === opcao.valor}
              showSelectedCheck
              onPress={() => escolherPeriodo(opcao.valor)}
            >
              {opcao.rotulo}
            </Chip>
          ))}
          <Chip icon="filter-variant" onPress={() => setAbrindoFiltros(true)}>
            {extras > 0 ? `Filtros (${extras})` : 'Filtros'}
          </Chip>
        </View>
        {temFiltros(filtros) ? (
          <Button compact icon="filter-remove-outline" onPress={limpar} style={styles.limpar}>
            Limpar filtros
          </Button>
        ) : null}
      </View>
      {consulta.isPending ? (
        <LoadingState message="Carregando o histórico..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <SectionList
          testID="lista-do-historico"
          sections={secoes}
          keyExtractor={(movimentacao) => movimentacao.id}
          stickySectionHeadersEnabled={false}
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
          contentContainerStyle={secoes.length === 0 ? styles.vazio : undefined}
          ListEmptyComponent={
            temFiltros(filtros) ? (
              <EmptyState
                title="Nenhuma movimentação com estes filtros"
                description="Mude o período ou toque em Limpar filtros."
              />
            ) : (
              <EmptyState
                title="Nenhuma movimentação"
                description="As entradas e saídas do canteiro aparecem aqui."
              />
            )
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
          }
          renderSectionHeader={({ section }) => <List.Subheader>{section.title}</List.Subheader>}
          renderItem={({ item }) => (
            <LinhaDeMovimentacao
              movimentacao={item}
              mostrarItem
              aoTocar={() => router.push(`/movimentacao/${item.id}`)}
            />
          )}
        />
      )}
      <FiltrosModal
        visivel={abrindoFiltros}
        canteiroId={canteiroId}
        filtros={filtros}
        aoMudar={mudar}
        aoLimpar={() => {
          limpar();
          setAbrindoFiltros(false);
        }}
        aoFechar={() => setAbrindoFiltros(false)}
      />
    </>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 4, gap: 4 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  limpar: { alignSelf: 'flex-start' },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
});
