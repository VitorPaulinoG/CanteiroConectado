import { useInfiniteQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import {
  ActivityIndicator,
  Appbar,
  HelperText,
  List,
  Modal,
  Portal,
  Searchbar,
  TextInput,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { juntarPaginas, paraAppError, proximaPagina } from '@/shared/api';
import { EmptyState } from '@/shared/components/EmptyState';
import { ErrorState } from '@/shared/components/ErrorState';
import { useAtrasado } from '@/shared/utils/useAtrasado';
import { useAppTheme } from '@/theme';

import { chavesColaboradores, listarColaboradores, type Colaborador } from './api';
import { ColaboradorModal } from './ColaboradorModal';

type Props = {
  canteiroId: string;
  valor: Colaborador | null;
  aoMudar: (colaborador: Colaborador | null) => void;
  rotulo?: string;
  erro?: string;
  desabilitado?: boolean;
};

export function SeletorColaborador({
  canteiroId,
  valor,
  aoMudar,
  rotulo = 'Colaborador',
  erro,
  desabilitado,
}: Props) {
  const [aberto, setAberto] = useState(false);

  return (
    <View>
      <TextInput
        label={rotulo}
        accessibilityLabel={rotulo}
        mode="outlined"
        value={valor?.nome ?? ''}
        editable={false}
        error={Boolean(erro)}
        disabled={desabilitado}
        onPressIn={() => !desabilitado && setAberto(true)}
        right={
          valor && !desabilitado ? (
            <TextInput.Icon
              icon="close"
              accessibilityLabel="Limpar colaborador"
              onPress={() => aoMudar(null)}
            />
          ) : (
            <TextInput.Icon
              icon="account-search-outline"
              accessibilityLabel="Escolher colaborador"
              onPress={() => !desabilitado && setAberto(true)}
              disabled={desabilitado}
            />
          )
        }
      />
      <HelperText type="error" visible={Boolean(erro)}>
        {erro ?? ''}
      </HelperText>
      <Portal>
        <Modal visible={aberto} onDismiss={() => setAberto(false)} style={styles.modalExterno}>
          {aberto ? (
            <ListaDeColaboradores
              canteiroId={canteiroId}
              aoEscolher={(colaborador) => {
                aoMudar(colaborador);
                setAberto(false);
              }}
              aoFechar={() => setAberto(false)}
            />
          ) : null}
        </Modal>
      </Portal>
    </View>
  );
}

type PropsLista = {
  canteiroId: string;
  aoEscolher: (colaborador: Colaborador) => void;
  aoFechar: () => void;
};

function ListaDeColaboradores({ canteiroId, aoEscolher, aoFechar }: PropsLista) {
  const theme = useAppTheme();
  const [busca, setBusca] = useState('');
  const [criando, setCriando] = useState(false);
  const termo = useAtrasado(busca.trim());
  const filtro = { q: termo || undefined, ativo: true };
  const consulta = useInfiniteQuery({
    queryKey: chavesColaboradores.lista(canteiroId, filtro),
    queryFn: ({ pageParam }) => listarColaboradores(canteiroId, filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
  const colaboradores = juntarPaginas(consulta.data?.pages);

  return (
    <SafeAreaView style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <Appbar.Header>
        <Appbar.Action icon="close" accessibilityLabel="Fechar" onPress={aoFechar} />
        <Appbar.Content title="Escolher colaborador" />
      </Appbar.Header>
      <View style={styles.busca}>
        <Searchbar
          placeholder="Buscar pelo nome"
          value={busca}
          onChangeText={setBusca}
          autoFocus
          accessibilityLabel="Buscar colaborador"
        />
      </View>
      <List.Item
        title={busca.trim() ? `Novo colaborador: ${busca.trim()}` : 'Novo colaborador'}
        left={(props) => <List.Icon {...props} icon="account-plus-outline" />}
        onPress={() => setCriando(true)}
      />
      {consulta.isPending ? (
        <ActivityIndicator style={styles.carregando} accessibilityLabel="Carregando" />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          data={colaboradores}
          keyExtractor={(colaborador) => colaborador.id}
          keyboardShouldPersistTaps="handled"
          onEndReached={() => {
            if (consulta.hasNextPage && !consulta.isFetchingNextPage) {
              void consulta.fetchNextPage();
            }
          }}
          onEndReachedThreshold={0.5}
          contentContainerStyle={colaboradores.length === 0 ? styles.vazio : undefined}
          ListEmptyComponent={
            <EmptyState
              title="Nenhum colaborador encontrado"
              description="Toque em Novo colaborador para cadastrar sem sair da tela."
            />
          }
          renderItem={({ item }) => (
            <List.Item
              title={item.nome}
              left={(props) => <List.Icon {...props} icon="account-outline" />}
              onPress={() => aoEscolher(item)}
            />
          )}
        />
      )}
      <ColaboradorModal
        visivel={criando}
        canteiroId={canteiroId}
        nomeInicial={busca.trim()}
        aoSalvar={(colaborador) => {
          setCriando(false);
          aoEscolher(colaborador);
        }}
        aoFechar={() => setCriando(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  modalExterno: { margin: 0 },
  tela: { flex: 1 },
  busca: { padding: 16, paddingBottom: 8 },
  carregando: { padding: 24 },
  vazio: { flexGrow: 1 },
});
