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
  Text,
  TextInput,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { juntarPaginas, paraAppError, proximaPagina, type Pagina } from '@/shared/api';
import { EmptyState } from '@/shared/components/EmptyState';
import { ErrorState } from '@/shared/components/ErrorState';
import { StatusBadge } from '@/shared/components/StatusBadge';
import {
  chavesEstoque,
  listarEstoque,
  seloDoStatus,
  type ItemEstoque,
  type StatusEstoque,
} from '@/shared/estoque';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { useAtrasado } from '@/shared/utils/useAtrasado';
import { useAppTheme } from '@/theme';

import { chavesItens, listarItens, ROTULO_DO_TIPO, type Item, type TipoItem } from './api';

export type ItemSelecionado = {
  itemId: string;
  codigo: string;
  descricao: string;
  unidadeSimbolo: string;
  tipo: TipoItem;
  saldo?: number;
  status?: StatusEstoque;
};

type Modo = { modo: 'catalogo' } | { modo: 'estoque'; canteiroId: string; exigirSaldo?: boolean };

type Props = Modo & {
  valor: ItemSelecionado | null;
  aoMudar: (item: ItemSelecionado) => void;
  rotulo?: string;
  erro?: string;
  desabilitado?: boolean;
};

function doCatalogo(item: Item): ItemSelecionado {
  return {
    itemId: item.id,
    codigo: item.codigo,
    descricao: item.descricao,
    unidadeSimbolo: item.unidadeSimbolo,
    tipo: item.tipo,
  };
}

function doEstoque(item: ItemEstoque): ItemSelecionado {
  return {
    itemId: item.itemId,
    codigo: item.codigo,
    descricao: item.descricao,
    unidadeSimbolo: item.unidadeSimbolo,
    tipo: item.tipo,
    saldo: item.saldo,
    status: item.status,
  };
}

export function SeletorItem({
  valor,
  aoMudar,
  rotulo = 'Item',
  erro,
  desabilitado,
  ...modo
}: Props) {
  const [aberto, setAberto] = useState(false);

  return (
    <View>
      <TextInput
        label={rotulo}
        accessibilityLabel={rotulo}
        mode="outlined"
        value={valor ? `${valor.descricao} (${valor.codigo})` : ''}
        editable={false}
        error={Boolean(erro)}
        disabled={desabilitado}
        onPressIn={() => !desabilitado && setAberto(true)}
        right={
          <TextInput.Icon
            icon="magnify"
            accessibilityLabel="Escolher item"
            onPress={() => !desabilitado && setAberto(true)}
            disabled={desabilitado}
          />
        }
      />
      <HelperText
        type={erro ? 'error' : 'info'}
        visible={Boolean(erro) || valor?.saldo !== undefined}
      >
        {erro ??
          (valor?.saldo !== undefined
            ? `Saldo no canteiro: ${formatarQuantidade(valor.saldo, valor.unidadeSimbolo)}`
            : '')}
      </HelperText>
      <SeletorItemModal
        {...modo}
        visivel={aberto}
        aoEscolher={(item) => {
          aoMudar(item);
          setAberto(false);
        }}
        aoFechar={() => setAberto(false)}
      />
    </View>
  );
}

type PropsModal = Modo & {
  visivel: boolean;
  aoEscolher: (item: ItemSelecionado) => void;
  aoFechar: () => void;
};

export function SeletorItemModal({ visivel, aoEscolher, aoFechar, ...modo }: PropsModal) {
  return (
    <Portal>
      <Modal visible={visivel} onDismiss={aoFechar} style={styles.modal}>
        {visivel ? <Lista {...modo} aoEscolher={aoEscolher} aoFechar={aoFechar} /> : null}
      </Modal>
    </Portal>
  );
}

type PropsLista = Modo & {
  aoEscolher: (item: ItemSelecionado) => void;
  aoFechar: () => void;
};

function useBusca(modo: Modo, termo: string) {
  const q = termo || undefined;
  const canteiroId = modo.modo === 'estoque' ? modo.canteiroId : '';
  return useInfiniteQuery<Pagina<ItemSelecionado>>({
    queryKey:
      modo.modo === 'estoque'
        ? [...chavesEstoque.lista(canteiroId, { q }), 'seletor']
        : [...chavesItens.lista({ q, ativo: true }), 'seletor'],
    queryFn: async ({ pageParam }) => {
      const pagina = pageParam as number;
      if (modo.modo === 'estoque') {
        const resultado = await listarEstoque(canteiroId, { q }, pagina);
        return { ...resultado, conteudo: resultado.conteudo.map(doEstoque) };
      }
      const resultado = await listarItens({ q, ativo: true }, pagina);
      return { ...resultado, conteudo: resultado.conteudo.map(doCatalogo) };
    },
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });
}

function Lista({ aoEscolher, aoFechar, ...modo }: PropsLista) {
  const theme = useAppTheme();
  const [busca, setBusca] = useState('');
  const termo = useAtrasado(busca.trim());
  const consulta = useBusca(modo, termo);
  const itens = juntarPaginas(consulta.data?.pages);
  const exigirSaldo = modo.modo === 'estoque' && modo.exigirSaldo;

  return (
    <SafeAreaView style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <Appbar.Header>
        <Appbar.Action icon="close" accessibilityLabel="Fechar" onPress={aoFechar} />
        <Appbar.Content title="Escolher item" />
      </Appbar.Header>
      <View style={styles.busca}>
        <Searchbar
          placeholder="Código ou descrição"
          value={busca}
          onChangeText={setBusca}
          autoFocus
          accessibilityLabel="Buscar item"
        />
      </View>
      {consulta.isPending ? (
        <ActivityIndicator style={styles.carregando} accessibilityLabel="Carregando" />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <FlatList
          testID="lista-do-seletor-de-item"
          data={itens}
          keyExtractor={(item) => item.itemId}
          keyboardShouldPersistTaps="handled"
          initialNumToRender={12}
          windowSize={7}
          onEndReached={() => {
            if (consulta.hasNextPage && !consulta.isFetchingNextPage) {
              void consulta.fetchNextPage();
            }
          }}
          onEndReachedThreshold={0.5}
          contentContainerStyle={itens.length === 0 ? styles.vazio : undefined}
          ListEmptyComponent={
            <EmptyState
              title="Nenhum item encontrado"
              description="Tente outro código ou descrição."
            />
          }
          ListFooterComponent={
            consulta.isFetchingNextPage ? (
              <ActivityIndicator style={styles.carregando} />
            ) : undefined
          }
          renderItem={({ item }) => {
            const semSaldo = exigirSaldo && (item.saldo ?? 0) <= 0;
            return (
              <List.Item
                title={item.descricao}
                description={
                  item.saldo !== undefined
                    ? `${item.codigo} · Saldo ${formatarQuantidade(item.saldo, item.unidadeSimbolo)}`
                    : `${item.codigo} · ${ROTULO_DO_TIPO[item.tipo]} · ${item.unidadeSimbolo}`
                }
                disabled={semSaldo}
                accessibilityState={{ disabled: semSaldo }}
                style={semSaldo ? styles.desabilitado : undefined}
                onPress={() => aoEscolher(item)}
                right={() =>
                  item.status ? (
                    <View style={styles.selo}>
                      <StatusBadge status={seloDoStatus(item.status)} />
                    </View>
                  ) : null
                }
              />
            );
          }}
        />
      )}
      {exigirSaldo ? (
        <Text variant="bodySmall" style={styles.dica}>
          Itens sem saldo no canteiro aparecem apagados e não podem ser escolhidos.
        </Text>
      ) : null}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  modal: { margin: 0 },
  tela: { flex: 1 },
  busca: { padding: 16, paddingBottom: 8 },
  carregando: { padding: 24 },
  vazio: { flexGrow: 1 },
  selo: { justifyContent: 'center' },
  desabilitado: { opacity: 0.45 },
  dica: { padding: 16 },
});
