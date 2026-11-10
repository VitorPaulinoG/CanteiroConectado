import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, Divider, List, Text } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, StatusBadge, TelaComCabecalho } from '@/shared';
import { paraAppError } from '@/shared/api';
import { useCanteiroIdAtivo } from '@/shared/canteiro';
import {
  buscarDetalheEstoque,
  chavesEstoque,
  LinhaDeMovimentacao,
  seloDoStatus,
  type DetalheEstoque,
} from '@/shared/estoque';
import { ROTULO_DO_TIPO } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { formatarQuantidade } from '@/shared/utils/quantidade';

type Props = { itemId: string };

export function DetalheItemTela({ itemId }: Props) {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const consulta = useQuery({
    queryKey: chavesEstoque.detalhe(canteiroId, itemId),
    queryFn: () => buscarDetalheEstoque(canteiroId, itemId),
  });

  return (
    <TelaComCabecalho
      titulo={consulta.data?.descricao ?? 'Item'}
      subtitulo={consulta.data?.codigo}
      aoVoltar={() => router.back()}
    >
      {consulta.isPending ? (
        <LoadingState message="Carregando o item..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <Conteudo
          detalhe={consulta.data}
          atualizando={consulta.isRefetching}
          aoAtualizar={() => void consulta.refetch()}
        />
      )}
    </TelaComCabecalho>
  );
}

type PropsConteudo = { detalhe: DetalheEstoque; atualizando: boolean; aoAtualizar: () => void };

function Conteudo({ detalhe, atualizando, aoAtualizar }: PropsConteudo) {
  const router = useRouter();

  return (
    <>
      <ScrollView
        contentContainerStyle={styles.conteudo}
        refreshControl={<RefreshControl refreshing={atualizando} onRefresh={aoAtualizar} />}
      >
        <Card mode="contained" style={styles.cartao}>
          <Card.Content style={styles.saldo}>
            <View style={styles.linha}>
              <Text variant="labelLarge">Saldo no canteiro</Text>
              <StatusBadge status={seloDoStatus(detalhe.status)} />
            </View>
            <Text variant="displaySmall" accessibilityLabel="Saldo">
              {formatarQuantidade(detalhe.saldo, detalhe.unidadeSimbolo)}
            </Text>
            <Text variant="bodyLarge">
              {detalhe.quantidadeMinima > 0
                ? `Mínimo: ${formatarQuantidade(detalhe.quantidadeMinima, detalhe.unidadeSimbolo)}`
                : 'Sem mínimo definido'}
            </Text>
          </Card.Content>
        </Card>

        <List.Section>
          <List.Item title="Tipo" description={ROTULO_DO_TIPO[detalhe.tipo]} />
          <List.Item title="Unidade" description={detalhe.unidadeSimbolo} />
          {!detalhe.ativo ? (
            <List.Item title="Situação" description="Item desativado no catálogo" />
          ) : null}
        </List.Section>

        <Divider />

        <List.Section>
          <List.Subheader>Últimas movimentações</List.Subheader>
          {detalhe.ultimasMovimentacoes.length === 0 ? (
            <EmptyState
              title="Sem movimentações"
              description="As entradas e saídas deste item no canteiro aparecem aqui."
            />
          ) : (
            detalhe.ultimasMovimentacoes.map((movimentacao) => (
              <LinhaDeMovimentacao key={movimentacao.id} movimentacao={movimentacao} />
            ))
          )}
        </List.Section>
      </ScrollView>

      {detalhe.ativo ? (
        <View style={styles.rodape}>
          <SomentePerfil perfis={['ALMOXARIFE']}>
            <Button
              mode="contained"
              icon="arrow-up-bold-circle-outline"
              disabled={detalhe.saldo <= 0}
              contentStyle={styles.botao}
              onPress={() => router.push(`/movimentar/saida?itemId=${detalhe.itemId}`)}
            >
              Registrar saída
            </Button>
          </SomentePerfil>
          <SomentePerfil perfis={['GESTOR']}>
            <Button
              mode="contained"
              icon="clipboard-plus-outline"
              contentStyle={styles.botao}
              onPress={() => router.push(`/requisicoes/nova?itemId=${detalhe.itemId}`)}
            >
              Solicitar material
            </Button>
          </SomentePerfil>
        </View>
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  conteudo: { padding: 16, paddingBottom: 24 },
  cartao: { marginBottom: 8 },
  saldo: { gap: 8 },
  linha: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
