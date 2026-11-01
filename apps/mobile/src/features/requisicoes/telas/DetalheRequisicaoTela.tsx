import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, Divider, List, Text } from 'react-native-paper';

import { ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { paraAppError } from '@/shared/api';
import { formatarData, formatarDataHora } from '@/shared/utils/format';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { useAtualizarAoFocar } from '@/shared/utils/useAtualizarAoFocar';
import { useAppTheme } from '@/theme';

import { buscarRequisicao, chavesRequisicoes, type Requisicao } from '../api';
import { DecisaoModal } from '../componentes/DecisaoModal';
import { SeloDaRequisicao } from '../componentes/SeloDaRequisicao';
import { DESCRICAO_DO_EVENTO, ICONE_DO_STATUS } from '../status';

type Props = { id: string };

export function DetalheRequisicaoTela({ id }: Props) {
  const router = useRouter();
  const consulta = useQuery({
    queryKey: chavesRequisicoes.detalhe(id),
    queryFn: () => buscarRequisicao(id),
  });
  useAtualizarAoFocar(() => consulta.refetch());

  return (
    <TelaComCabecalho
      titulo={consulta.data?.numero ?? 'Requisição'}
      subtitulo={consulta.data?.canteiroNome}
      aoVoltar={() => router.back()}
    >
      {consulta.isPending ? (
        <LoadingState message="Carregando a requisição..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <>
          <ScrollView
            contentContainerStyle={styles.conteudo}
            refreshControl={
              <RefreshControl
                refreshing={consulta.isRefetching}
                onRefresh={() => void consulta.refetch()}
              />
            }
          >
            <Conteudo requisicao={consulta.data} />
          </ScrollView>
          <Acoes requisicao={consulta.data} />
        </>
      )}
    </TelaComCabecalho>
  );
}

function Acoes({ requisicao }: { requisicao: Requisicao }) {
  const [decidindo, setDecidindo] = useState(false);

  if (!requisicao.acoes.decidir) {
    return null;
  }
  return (
    <View style={styles.rodape}>
      <Button
        mode="contained"
        icon="gavel"
        onPress={() => setDecidindo(true)}
        contentStyle={styles.botao}
      >
        Aprovar ou recusar
      </Button>
      <DecisaoModal
        requisicao={requisicao}
        visivel={decidindo}
        aoFechar={() => setDecidindo(false)}
      />
    </View>
  );
}

function Conteudo({ requisicao }: { requisicao: Requisicao }) {
  const theme = useAppTheme();

  return (
    <>
      <View style={styles.topo}>
        <SeloDaRequisicao status={requisicao.status} />
        <Text variant="bodyMedium">
          Pedida por {requisicao.solicitanteNome} em {formatarDataHora(requisicao.criadoEm)}
        </Text>
      </View>

      {requisicao.previsaoChegada && requisicao.status !== 'RECUSADA' ? (
        <Card
          mode="contained"
          style={{ backgroundColor: theme.status.normalContainer }}
          accessibilityLabel="Previsão de chegada"
        >
          <Card.Content>
            <Text variant="labelLarge" style={{ color: theme.status.onNormalContainer }}>
              Previsão de chegada
            </Text>
            <Text variant="headlineSmall" style={{ color: theme.status.onNormalContainer }}>
              {formatarData(requisicao.previsaoChegada)}
            </Text>
          </Card.Content>
        </Card>
      ) : null}

      {requisicao.status === 'RECUSADA' && requisicao.observacaoDecisao ? (
        <Card
          mode="contained"
          style={{ backgroundColor: theme.status.emptyContainer }}
          accessibilityLabel="Motivo da recusa"
        >
          <Card.Content>
            <Text variant="labelLarge" style={{ color: theme.status.onEmptyContainer }}>
              Motivo da recusa
            </Text>
            <Text variant="bodyLarge" style={{ color: theme.status.onEmptyContainer }}>
              {requisicao.observacaoDecisao}
            </Text>
          </Card.Content>
        </Card>
      ) : null}

      <List.Section>
        <List.Subheader>Itens</List.Subheader>
        {requisicao.itens.map((item) => (
          <List.Item
            key={item.itemId}
            title={item.descricao}
            description={item.codigo}
            right={() => (
              <Text variant="titleMedium" style={styles.quantidade}>
                {formatarQuantidade(item.quantidade, item.unidadeSimbolo)}
              </Text>
            )}
          />
        ))}
      </List.Section>

      <Divider />

      <List.Section>
        <List.Item title="Necessário até" description={formatarData(requisicao.necessarioAte)} />
        {requisicao.frenteObra ? (
          <List.Item title="Frente de obra" description={requisicao.frenteObra} />
        ) : null}
        {requisicao.observacao ? (
          <List.Item title="Observação" description={requisicao.observacao} />
        ) : null}
        {requisicao.decididoPorNome && requisicao.status !== 'RECUSADA' ? (
          <List.Item
            title={`Decidida por ${requisicao.decididoPorNome}`}
            description={requisicao.observacaoDecisao ?? undefined}
          />
        ) : null}
      </List.Section>

      <Divider />

      <List.Section>
        <List.Subheader>Linha do tempo</List.Subheader>
        {requisicao.historico.map((evento, indice) => (
          <List.Item
            key={`${evento.status}-${indice}`}
            title={`${DESCRICAO_DO_EVENTO[evento.status]} por ${evento.usuarioNome}`}
            description={[formatarDataHora(evento.criadoEm), evento.observacao]
              .filter(Boolean)
              .join('\n')}
            descriptionNumberOfLines={3}
            left={(props) => <List.Icon {...props} icon={ICONE_DO_STATUS[evento.status]} />}
          />
        ))}
      </List.Section>
    </>
  );
}

const styles = StyleSheet.create({
  conteudo: { padding: 16, gap: 12, paddingBottom: 32 },
  topo: { gap: 8 },
  quantidade: { alignSelf: 'center' },
  rodape: { padding: 16, gap: 8 },
  botao: { minHeight: 48 },
});
