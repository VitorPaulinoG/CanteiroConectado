import { StyleSheet, View } from 'react-native';
import { HelperText, IconButton, TextInput } from 'react-native-paper';

import { formatarNumero, lerQuantidade } from '@/shared/utils/quantidade';

type Props = {
  valor: string;
  aoMudar: (valor: string) => void;
  unidadeSimbolo?: string;
  rotulo?: string;
  erro?: string;
  dica?: string;
  desabilitado?: boolean;
};

export function CampoQuantidade({
  valor,
  aoMudar,
  unidadeSimbolo,
  rotulo = 'Quantidade',
  erro,
  dica,
  desabilitado,
}: Props) {
  const atual = lerQuantidade(valor) ?? 0;

  function somar(passo: number) {
    aoMudar(formatarNumero(Math.max(0, atual + passo)).replace(/\./g, ''));
  }

  return (
    <View>
      <View style={styles.linha}>
        <IconButton
          icon="minus"
          mode="contained-tonal"
          size={28}
          accessibilityLabel="Diminuir quantidade"
          disabled={desabilitado || atual <= 1}
          onPress={() => somar(-1)}
        />
        <TextInput
          label={rotulo}
          accessibilityLabel={rotulo}
          mode="outlined"
          value={valor}
          onChangeText={aoMudar}
          keyboardType="decimal-pad"
          error={Boolean(erro)}
          disabled={desabilitado}
          style={styles.campo}
          right={unidadeSimbolo ? <TextInput.Affix text={unidadeSimbolo} /> : undefined}
        />
        <IconButton
          icon="plus"
          mode="contained-tonal"
          size={28}
          accessibilityLabel="Aumentar quantidade"
          disabled={desabilitado}
          onPress={() => somar(1)}
        />
      </View>
      <HelperText type={erro ? 'error' : 'info'} visible={Boolean(erro ?? dica)}>
        {erro ?? dica ?? ''}
      </HelperText>
    </View>
  );
}

const styles = StyleSheet.create({
  linha: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  campo: { flex: 1 },
});
