import { StyleSheet, View } from 'react-native';
import { Chip, List } from 'react-native-paper';

import { formatarDataHora } from '@/shared/utils/format';
import { formatarQuantidade } from '@/shared/utils/quantidade';

import type { Movimentacao } from './api';
import {
  ICONE_DO_TIPO_DE_MOVIMENTACAO,
  ROTULO_DO_MOTIVO,
  ROTULO_DO_TIPO_DE_MOVIMENTACAO,
} from './rotulos';

type Props = {
  movimentacao: Movimentacao;
  mostrarItem?: boolean;
  aoTocar?: () => void;
};

export function LinhaDeMovimentacao({ movimentacao, mostrarItem, aoTocar }: Props) {
  const sinal = movimentacao.tipo === 'ENTRADA' ? '+' : '−';
  const quantidade = `${sinal}${formatarQuantidade(movimentacao.quantidade, movimentacao.unidadeSimbolo)}`;
  const titulo = mostrarItem
    ? `${movimentacao.itemDescricao} · ${quantidade}`
    : `${ROTULO_DO_TIPO_DE_MOVIMENTACAO[movimentacao.tipo]} ${quantidade}`;
  const partes = [
    ROTULO_DO_MOTIVO[movimentacao.motivo],
    movimentacao.colaboradorNome,
    movimentacao.frenteObra,
  ].filter(Boolean);
  const destaque =
    movimentacao.motivo === 'ESTORNO' ||
    movimentacao.motivo === 'AJUSTE' ||
    movimentacao.estornadaPorId;

  return (
    <List.Item
      title={titulo}
      description={`${partes.join(' · ')}\n${movimentacao.registradoPorNome} · ${formatarDataHora(movimentacao.criadoEm)}`}
      descriptionNumberOfLines={2}
      onPress={aoTocar}
      left={(props) => (
        <List.Icon {...props} icon={ICONE_DO_TIPO_DE_MOVIMENTACAO[movimentacao.tipo]} />
      )}
      right={() =>
        destaque ? (
          <View style={styles.selo}>
            <Chip compact>
              {movimentacao.estornadaPorId ? 'Estornada' : ROTULO_DO_MOTIVO[movimentacao.motivo]}
            </Chip>
          </View>
        ) : null
      }
    />
  );
}

const styles = StyleSheet.create({
  selo: { justifyContent: 'center' },
});
