import { StyleSheet, View } from 'react-native';
import { Card, Text } from 'react-native-paper';

import { formatarData } from '@/shared/utils/format';
import { formatarQuantidade } from '@/shared/utils/quantidade';

import type { ResumoDaRequisicao } from '../api';
import { SeloDaRequisicao } from './SeloDaRequisicao';

type Props = { requisicao: ResumoDaRequisicao; mostrarSolicitante?: boolean; aoTocar: () => void };

export function resumoDosItens(requisicao: Pick<ResumoDaRequisicao, 'itens'>): string {
  const [primeiro, ...resto] = requisicao.itens;
  if (!primeiro) {
    return '';
  }
  const texto = `${primeiro.descricao} (${formatarQuantidade(primeiro.quantidade, primeiro.unidadeSimbolo)})`;
  return resto.length === 0 ? texto : `${texto} e mais ${resto.length}`;
}

export function CartaoDeRequisicao({ requisicao, mostrarSolicitante, aoTocar }: Props) {
  return (
    <Card
      mode="outlined"
      onPress={aoTocar}
      style={styles.cartao}
      accessibilityLabel={requisicao.numero}
    >
      <Card.Content style={styles.conteudo}>
        <View style={styles.topo}>
          <Text variant="titleMedium">{requisicao.numero}</Text>
          <SeloDaRequisicao status={requisicao.status} />
        </View>
        <Text variant="bodyLarge">{resumoDosItens(requisicao)}</Text>
        <Text variant="bodyMedium">
          Necessário até {formatarData(requisicao.necessarioAte)}
          {requisicao.previsaoChegada
            ? ` · Chega em ${formatarData(requisicao.previsaoChegada)}`
            : ''}
        </Text>
        {mostrarSolicitante ? (
          <Text variant="bodyMedium">Pedida por {requisicao.solicitanteNome}</Text>
        ) : null}
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  cartao: { marginHorizontal: 16, marginVertical: 6 },
  conteudo: { gap: 4 },
  topo: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
});
