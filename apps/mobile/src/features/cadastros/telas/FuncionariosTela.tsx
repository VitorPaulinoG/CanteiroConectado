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
import { rotuloDoPerfil, SomentePerfil, type Perfil } from '@/shared/sessao';
import { useAtrasado } from '@/shared/utils/useAtrasado';

import { chavesFuncionarios, listarFuncionarios, type Funcionario } from '../usuarios';

const FILTROS_PERFIL: { valor: Perfil | undefined; rotulo: string }[] = [
  { valor: undefined, rotulo: 'Todos' },
  { valor: 'ALMOXARIFE', rotulo: 'Almoxarifes' },
  { valor: 'GESTOR', rotulo: 'Gestores' },
];

function Selos({ funcionario }: { funcionario: Funcionario }) {
  return (
    <View style={styles.selos}>
      {funcionario.credencialInicialDisponivel ? (
        <Chip compact icon="key-outline">
          Senha temporária
        </Chip>
      ) : null}
      {!funcionario.ativo ? (
        <Chip compact icon="pause-circle-outline">
          Inativo
        </Chip>
      ) : null}
    </View>
  );
}

export function FuncionariosTela() {
  const router = useRouter();
  const [busca, setBusca] = useState('');
  const [perfil, setPerfil] = useState<Perfil | undefined>(undefined);
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const termo = useAtrasado(busca.trim());
  const filtro = { q: termo || undefined, perfil, ativo: situacaoParaAtivo(situacao) };

  const consulta = useInfiniteQuery({
    queryKey: chavesFuncionarios.lista(filtro),
    queryFn: ({ pageParam }) => listarFuncionarios(filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const funcionarios = juntarPaginas(consulta.data?.pages);

  return (
    <TelaComCabecalho titulo="Funcionários" aoVoltar={() => router.back()}>
      <SomentePerfil administrador alternativa={<AcessoNegado />}>
        <View style={styles.filtros}>
          <Searchbar
            placeholder="Buscar por nome ou e-mail"
            value={busca}
            onChangeText={setBusca}
            accessibilityLabel="Buscar funcionário"
          />
          <View style={styles.chips}>
            {FILTROS_PERFIL.map((opcao) => (
              <Chip
                key={opcao.rotulo}
                selected={perfil === opcao.valor}
                showSelectedCheck
                onPress={() => setPerfil(opcao.valor)}
              >
                {opcao.rotulo}
              </Chip>
            ))}
          </View>
          <FiltroSituacao valor={situacao} aoMudar={setSituacao} />
        </View>
        {consulta.isPending ? (
          <LoadingState message="Carregando funcionários..." />
        ) : consulta.isError ? (
          <ErrorState
            message={paraAppError(consulta.error).message}
            onRetry={() => consulta.refetch()}
          />
        ) : (
          <FlatList
            data={funcionarios}
            keyExtractor={(funcionario) => funcionario.id}
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
            contentContainerStyle={funcionarios.length === 0 ? styles.vazio : styles.lista}
            ListEmptyComponent={
              <EmptyState
                title="Nenhum funcionário"
                description="Ajuste a busca ou os filtros, ou cadastre um funcionário."
              />
            }
            ListFooterComponent={
              consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
            }
            renderItem={({ item }) => (
              <List.Item
                title={item.nome}
                description={`${item.email}\n${rotuloDoPerfil(item)}`}
                descriptionNumberOfLines={2}
                left={(props) => <List.Icon {...props} icon="account-outline" />}
                right={() => <Selos funcionario={item} />}
                onPress={() => router.push(`/cadastros/usuarios/${item.id}`)}
              />
            )}
          />
        )}
        <FAB
          icon="account-plus-outline"
          label="Novo funcionário"
          style={styles.fab}
          onPress={() => router.push('/cadastros/usuarios/novo')}
        />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 8, gap: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  selos: { justifyContent: 'center', gap: 4 },
  lista: { paddingBottom: 96 },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
  fab: { position: 'absolute', right: 16, bottom: 16 },
});
