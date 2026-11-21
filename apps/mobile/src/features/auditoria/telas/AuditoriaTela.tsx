import { useInfiniteQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Chip, List, Menu, Text } from 'react-native-paper';

import { AcessoNegado, EmptyState, ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { SomentePerfil } from '@/shared/sessao';
import { formatarDataHora } from '@/shared/utils/format';
import { useAppTheme } from '@/theme';

import {
  chavesAuditoria,
  listarAuditoria,
  type AcaoAuditoria,
  type EventoDeAuditoria,
  type FiltroDaAuditoria,
} from '../api';
import {
  GRUPOS_DE_ACAO,
  ICONE_DA_ACAO,
  ROTULO_DA_ACAO,
  ROTULO_DA_ENTIDADE,
  rotuloDoDetalhe,
  textoDoDetalhe,
} from '../rotulos';

type Periodo = 'tudo' | 'hoje' | '7dias';

const PERIODOS: { valor: Periodo; rotulo: string }[] = [
  { valor: 'tudo', rotulo: 'Tudo' },
  { valor: 'hoje', rotulo: 'Hoje' },
  { valor: '7dias', rotulo: '7 dias' },
];

export function filtroDaAuditoria(
  acao: AcaoAuditoria | null,
  periodo: Periodo,
  agora: Date = new Date(),
): FiltroDaAuditoria {
  const filtro: FiltroDaAuditoria = { acao: acao ?? undefined };
  if (periodo !== 'tudo') {
    const inicio = new Date(agora);
    inicio.setHours(0, 0, 0, 0);
    if (periodo === '7dias') {
      inicio.setDate(inicio.getDate() - 6);
    }
    filtro.de = inicio.toISOString();
  }
  return filtro;
}

export function AuditoriaTela() {
  const router = useRouter();
  return (
    <TelaComCabecalho titulo="Trilha de auditoria" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['GESTOR']} alternativa={<AcessoNegado />}>
        <Conteudo />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

function Conteudo() {
  const [acao, setAcao] = useState<AcaoAuditoria | null>(null);
  const [periodo, setPeriodo] = useState<Periodo>('tudo');
  const [menuAberto, setMenuAberto] = useState(false);
  const filtro = useMemo(() => filtroDaAuditoria(acao, periodo), [acao, periodo]);

  const consulta = useInfiniteQuery({
    queryKey: chavesAuditoria.lista(filtro),
    queryFn: ({ pageParam }) => listarAuditoria(filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const eventos = juntarPaginas(consulta.data?.pages);

  return (
    <>
      <View style={styles.filtros}>
        <View style={styles.chips}>
          {PERIODOS.map((opcao) => (
            <Chip
              key={opcao.valor}
              selected={periodo === opcao.valor}
              showSelectedCheck
              onPress={() => setPeriodo(opcao.valor)}
            >
              {opcao.rotulo}
            </Chip>
          ))}
        </View>
        <Menu
          visible={menuAberto}
          onDismiss={() => setMenuAberto(false)}
          anchor={
            <Button
              mode="outlined"
              icon="filter-variant"
              onPress={() => setMenuAberto(true)}
              accessibilityLabel="Filtrar por ação"
            >
              {acao ? ROTULO_DA_ACAO[acao] : 'Todas as ações'}
            </Button>
          }
        >
          <Menu.Item
            title="Todas as ações"
            onPress={() => {
              setAcao(null);
              setMenuAberto(false);
            }}
          />
          {GRUPOS_DE_ACAO.flatMap((grupo) =>
            grupo.acoes.map((opcao) => (
              <Menu.Item
                key={opcao}
                title={ROTULO_DA_ACAO[opcao]}
                onPress={() => {
                  setAcao(opcao);
                  setMenuAberto(false);
                }}
              />
            )),
          )}
        </Menu>
      </View>
      {consulta.isPending ? (
        <LoadingState message="Carregando a trilha..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          testID="lista-da-auditoria"
          data={eventos}
          keyExtractor={(evento) => evento.id}
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
          contentContainerStyle={eventos.length === 0 ? styles.vazio : undefined}
          ListEmptyComponent={
            <EmptyState
              title="Nenhum evento"
              description="Mude a ação ou o período para ver outros eventos."
            />
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? <ActivityIndicator style={styles.rodape} /> : undefined
          }
          renderItem={({ item }) => <Evento evento={item} />}
        />
      )}
    </>
  );
}

function Evento({ evento }: { evento: EventoDeAuditoria }) {
  const theme = useAppTheme();
  const detalhes = Object.entries(evento.detalhes ?? {});

  return (
    <List.Accordion
      title={ROTULO_DA_ACAO[evento.acao] ?? evento.acao}
      description={`${evento.usuarioNome ?? 'Sistema'} · ${formatarDataHora(evento.criadoEm)}`}
      left={(props) => (
        <List.Icon {...props} icon={ICONE_DA_ACAO[evento.acao] ?? 'clipboard-text-clock-outline'} />
      )}
      accessibilityLabel={`${ROTULO_DA_ACAO[evento.acao] ?? evento.acao} por ${evento.usuarioNome ?? 'Sistema'}`}
    >
      <View style={[styles.detalhe, { backgroundColor: theme.colors.surfaceVariant }]}>
        <Linha rotulo="Quem fez" valor={evento.usuarioNome ?? 'Sistema ou pessoa sem login'} />
        <Linha
          rotulo={ROTULO_DA_ENTIDADE[evento.entidade] ?? evento.entidade}
          valor={evento.entidadeDescricao ?? '—'}
        />
        {detalhes.map(([chave, valor]) => (
          <Linha key={chave} rotulo={rotuloDoDetalhe(chave)} valor={textoDoDetalhe(valor)} />
        ))}
        <Linha rotulo="Requisição" valor={evento.requestId ?? '—'} />
      </View>
    </List.Accordion>
  );
}

function Linha({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <View style={styles.linha}>
      <Text variant="labelLarge" style={styles.rotulo}>
        {rotulo}
      </Text>
      <Text variant="bodyMedium" style={styles.valor} selectable>
        {valor}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  filtros: { padding: 16, paddingBottom: 8, gap: 8 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  vazio: { flexGrow: 1 },
  rodape: { padding: 16 },
  detalhe: { marginHorizontal: 16, marginBottom: 12, padding: 12, borderRadius: 12, gap: 6 },
  linha: { flexDirection: 'row', gap: 12 },
  rotulo: { width: 120 },
  valor: { flex: 1 },
});
