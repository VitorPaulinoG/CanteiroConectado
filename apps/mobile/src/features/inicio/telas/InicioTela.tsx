import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Avatar, Button, Card, List, Text } from 'react-native-paper';

import { ErrorState, LoadingState, StatusBadge } from '@/shared';
import { paraAppError } from '@/shared/api';
import { TelaDoCanteiro, useCanteiroIdAtivo } from '@/shared/canteiro';
import { chavesEstoque, seloDoStatus } from '@/shared/estoque';
import { SomentePerfil, useUsuario } from '@/shared/sessao';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { useAtualizarAoFocar } from '@/shared/utils/useAtualizarAoFocar';
import { useAppTheme } from '@/theme';

import { buscarResumo, type Resumo, type StatusDaMinhaRequisicao } from '../api';

const ROTULOS_DAS_MINHAS: [StatusDaMinhaRequisicao, string, string][] = [
  ['PENDENTE', 'pendente', 'pendentes'],
  ['APROVADA', 'aprovada', 'aprovadas'],
  ['RECUSADA', 'recusada', 'recusadas'],
];

function plural(quantidade: number, singular: string, varios: string) {
  return `${quantidade} ${quantidade === 1 ? singular : varios}`;
}

export function InicioTela() {
  const usuario = useUsuario();
  const canteiroId = useCanteiroIdAtivo();
  const consulta = useQuery({
    queryKey: chavesEstoque.resumo(canteiroId),
    queryFn: () => buscarResumo(canteiroId),
  });
  useAtualizarAoFocar(() => consulta.refetch());

  return (
    <TelaDoCanteiro titulo={`Olá, ${usuario.nome.split(' ')[0]}`}>
      {consulta.isPending ? (
        <LoadingState message="Carregando o resumo do canteiro..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <ScrollView
          contentContainerStyle={styles.conteudo}
          refreshControl={
            <RefreshControl
              refreshing={consulta.isRefetching}
              onRefresh={() => void consulta.refetch()}
            />
          }
        >
          <Conteudo resumo={consulta.data} />
        </ScrollView>
      )}
    </TelaDoCanteiro>
  );
}

function Conteudo({ resumo }: { resumo: Resumo }) {
  const router = useRouter();
  const theme = useAppTheme();
  const emAlerta = resumo.zerados + resumo.abaixoDoMinimo;

  return (
    <>
      <SomentePerfil perfis={['ALMOXARIFE']}>
        <View style={styles.atalhos}>
          <Button
            mode="contained"
            icon="arrow-up-bold"
            style={styles.atalho}
            contentStyle={styles.botao}
            onPress={() => router.push('/movimentar/saida')}
          >
            Saída
          </Button>
          <Button
            mode="contained-tonal"
            icon="arrow-down-bold"
            style={styles.atalho}
            contentStyle={styles.botao}
            onPress={() => router.push('/movimentar/entrada')}
          >
            Entrada
          </Button>
        </View>
      </SomentePerfil>

      <Card
        mode="elevated"
        onPress={emAlerta > 0 ? () => router.navigate('/estoque?filtro=alerta') : undefined}
        accessibilityLabel="Alertas de estoque"
      >
        <Card.Title
          title="Estoque"
          titleVariant="titleLarge"
          left={(props) => (
            <Avatar.Icon
              {...props}
              icon={emAlerta > 0 ? 'alert' : 'check'}
              color={emAlerta > 0 ? theme.status.onEmptyContainer : theme.status.onNormalContainer}
              style={{
                backgroundColor:
                  emAlerta > 0 ? theme.status.emptyContainer : theme.status.normalContainer,
              }}
            />
          )}
        />
        <Card.Content style={styles.cartao}>
          {emAlerta === 0 ? (
            <Text variant="bodyLarge">Tudo certo por aqui</Text>
          ) : (
            <>
              <Text variant="titleMedium">
                {plural(resumo.zerados, 'zerado', 'zerados')} ·{' '}
                {plural(resumo.abaixoDoMinimo, 'abaixo do mínimo', 'abaixo do mínimo')}
              </Text>
              {resumo.maisCriticos.map((item) => (
                <List.Item
                  key={item.itemId}
                  title={item.descricao}
                  description={`Saldo ${formatarQuantidade(item.saldo, item.unidadeSimbolo)} · mínimo ${formatarQuantidade(item.quantidadeMinima, item.unidadeSimbolo)}`}
                  right={() => (
                    <View style={styles.selo}>
                      <StatusBadge status={seloDoStatus(item.status)} />
                    </View>
                  )}
                  onPress={() => router.push(`/estoque/${item.itemId}`)}
                />
              ))}
              <Text variant="labelLarge" style={{ color: theme.colors.primary }}>
                Ver todos os itens em alerta
              </Text>
            </>
          )}
        </Card.Content>
      </Card>

      <Card
        mode="elevated"
        onPress={() => router.navigate('/requisicoes?aba=para-decidir')}
        accessibilityLabel="Requisições para decidir"
      >
        <Card.Title
          title="Para decidir"
          titleVariant="titleLarge"
          subtitle={
            resumo.requisicoesParaDecidir === 0
              ? 'Nenhuma requisição esperando você'
              : plural(
                  resumo.requisicoesParaDecidir,
                  'requisição esperando você',
                  'requisições esperando você',
                )
          }
          left={(props) => <Avatar.Text {...props} label={String(resumo.requisicoesParaDecidir)} />}
        />
      </Card>

      <SomentePerfil perfis={['GESTOR']}>
        <Card
          mode="elevated"
          onPress={() => router.navigate('/requisicoes?aba=minhas')}
          accessibilityLabel="Minhas requisições"
        >
          <Card.Title title="Minhas requisições" titleVariant="titleLarge" />
          <Card.Content style={styles.minhas}>
            {ROTULOS_DAS_MINHAS.map(([status, singular, varios]) => (
              <Text key={status} variant="bodyLarge">
                {plural(resumo.minhasRequisicoes[status] ?? 0, singular, varios)}
              </Text>
            ))}
          </Card.Content>
        </Card>
      </SomentePerfil>
    </>
  );
}

const styles = StyleSheet.create({
  conteudo: { padding: 16, gap: 16, paddingBottom: 32 },
  atalhos: { flexDirection: 'row', gap: 12 },
  atalho: { flex: 1 },
  botao: { minHeight: 56 },
  cartao: { gap: 4 },
  selo: { justifyContent: 'center' },
  minhas: { flexDirection: 'row', flexWrap: 'wrap', gap: 16 },
});
