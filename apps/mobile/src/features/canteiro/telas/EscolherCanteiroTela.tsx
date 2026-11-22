import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { Appbar, Button, List, RadioButton } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { paraAppError } from '@/shared/api';
import { chavesCanteiros, listarCanteiros, useCanteiroAtivo } from '@/shared/canteiro';
import { useSessao, useUsuario } from '@/shared/sessao';

export function EscolherCanteiroTela() {
  const router = useRouter();
  const usuario = useUsuario();
  const { sair } = useSessao();
  const { status, canteiroId, escolher } = useCanteiroAtivo();
  const obrigatorio = status !== 'pronto';
  const [selecionado, setSelecionado] = useState<string | null>(canteiroId);
  const [salvando, setSalvando] = useState(false);
  const consulta = useQuery({
    queryKey: chavesCanteiros.lista(true),
    queryFn: () => listarCanteiros(true),
  });

  async function usar() {
    if (!selecionado) {
      return;
    }
    setSalvando(true);
    try {
      await escolher(selecionado);
      if (router.canGoBack()) {
        router.back();
      } else {
        router.replace('/inicio');
      }
    } finally {
      setSalvando(false);
    }
  }

  return (
    <TelaComCabecalho
      titulo="Escolher canteiro"
      aoVoltar={obrigatorio ? undefined : () => router.back()}
      acoes={
        obrigatorio ? (
          <Appbar.Action icon="logout" accessibilityLabel="Sair" onPress={() => void sair()} />
        ) : undefined
      }
    >
      {consulta.isPending ? (
        <LoadingState message="Carregando canteiros..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <>
          <FlatList
            data={consulta.data}
            keyExtractor={(canteiro) => canteiro.id}
            refreshControl={
              <RefreshControl
                refreshing={consulta.isRefetching}
                onRefresh={() => void consulta.refetch()}
              />
            }
            contentContainerStyle={consulta.data.length === 0 ? styles.vazio : undefined}
            ListHeaderComponent={
              consulta.data.length > 0 ? (
                <List.Subheader>Em qual canteiro você vai trabalhar?</List.Subheader>
              ) : undefined
            }
            ListEmptyComponent={
              <View style={styles.vazio}>
                <EmptyState
                  title="Nenhum canteiro ativo"
                  description={
                    usuario.administrador
                      ? 'Cadastre o primeiro canteiro para começar.'
                      : 'Peça ao administrador para cadastrar um canteiro.'
                  }
                />
                {usuario.administrador ? (
                  <Button
                    mode="contained"
                    style={styles.cadastrar}
                    onPress={() => router.push('/cadastros/canteiros/novo')}
                  >
                    Cadastrar canteiro
                  </Button>
                ) : null}
              </View>
            }
            renderItem={({ item }) => (
              <List.Item
                title={item.nome}
                description={item.endereco ?? undefined}
                onPress={() => setSelecionado(item.id)}
                accessibilityRole="radio"
                accessibilityState={{ checked: selecionado === item.id }}
                left={() => (
                  <RadioButton.Android
                    value={item.id}
                    status={selecionado === item.id ? 'checked' : 'unchecked'}
                    onPress={() => setSelecionado(item.id)}
                  />
                )}
              />
            )}
          />
          {consulta.data.length > 0 ? (
            <View style={styles.rodape}>
              <Button
                mode="contained"
                onPress={() => void usar()}
                loading={salvando}
                disabled={!selecionado || salvando}
                contentStyle={styles.botao}
              >
                Usar este canteiro
              </Button>
            </View>
          ) : null}
        </>
      )}
    </TelaComCabecalho>
  );
}

const styles = StyleSheet.create({
  vazio: { flexGrow: 1, justifyContent: 'center' },
  cadastrar: { alignSelf: 'center' },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
