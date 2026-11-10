import { StyleSheet, View } from 'react-native';
import { HelperText, IconButton, Text, TextInput } from 'react-native-paper';

import { formatarQuantidade } from '@/shared/utils/quantidade';

import type { LinhaDaRequisicao } from '../validations';

type Props = {
  linha: LinhaDaRequisicao;
  aoMudarQuantidade: (quantidade: string) => void;
  aoRemover: () => void;
  erro?: string;
  desabilitado?: boolean;
};

export function LinhaDeItem({ linha, aoMudarQuantidade, aoRemover, erro, desabilitado }: Props) {
  return (
    <View style={styles.linha}>
      <View style={styles.cabecalho}>
        <View style={styles.texto}>
          <Text variant="titleMedium">{linha.descricao}</Text>
          <Text variant="bodyMedium">
            {linha.codigo}
            {linha.saldo !== undefined
              ? ` · Saldo ${formatarQuantidade(linha.saldo, linha.unidadeSimbolo)}`
              : ''}
          </Text>
        </View>
        <IconButton
          icon="delete-outline"
          accessibilityLabel={`Remover ${linha.descricao}`}
          onPress={aoRemover}
          disabled={desabilitado}
        />
      </View>
      <TextInput
        label="Quantidade"
        accessibilityLabel={`Quantidade de ${linha.descricao}`}
        mode="outlined"
        value={linha.quantidade}
        onChangeText={aoMudarQuantidade}
        keyboardType="decimal-pad"
        error={Boolean(erro)}
        disabled={desabilitado}
        right={<TextInput.Affix text={linha.unidadeSimbolo} />}
      />
      <HelperText type="error" visible={Boolean(erro)}>
        {erro ?? ''}
      </HelperText>
    </View>
  );
}

const styles = StyleSheet.create({
  linha: { paddingVertical: 4 },
  cabecalho: { flexDirection: 'row', alignItems: 'center' },
  texto: { flex: 1 },
});
