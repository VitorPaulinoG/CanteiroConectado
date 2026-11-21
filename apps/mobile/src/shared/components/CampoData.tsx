import { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { View } from 'react-native';
import { HelperText, TextInput } from 'react-native-paper';

import { deData, paraData } from '@/shared/utils/data';
import { formatarData } from '@/shared/utils/format';

type Props = {
  rotulo: string;
  valor: string | null;
  aoMudar: (valor: string | null) => void;
  minimo?: string;
  erro?: string;
  dica?: string;
  desabilitado?: boolean;
  opcional?: boolean;
};

export function CampoData({
  rotulo,
  valor,
  aoMudar,
  minimo,
  erro,
  dica,
  desabilitado,
  opcional,
}: Props) {
  function abrir() {
    if (desabilitado) {
      return;
    }
    DateTimePickerAndroid.open({
      value: valor ? paraData(valor) : minimo ? paraData(minimo) : new Date(),
      mode: 'date',
      minimumDate: minimo ? paraData(minimo) : undefined,
      onChange: (evento, data) => {
        if (evento.type === 'set' && data) {
          aoMudar(deData(data));
        }
      },
    });
  }

  return (
    <View>
      <TextInput
        label={rotulo}
        accessibilityLabel={rotulo}
        mode="outlined"
        value={valor ? formatarData(valor) : ''}
        editable={false}
        error={Boolean(erro)}
        disabled={desabilitado}
        onPressIn={abrir}
        right={
          valor && opcional && !desabilitado ? (
            <TextInput.Icon
              icon="close"
              accessibilityLabel={`Limpar ${rotulo.toLowerCase()}`}
              onPress={() => aoMudar(null)}
            />
          ) : (
            <TextInput.Icon
              icon="calendar"
              accessibilityLabel={`Escolher ${rotulo.toLowerCase()}`}
              onPress={abrir}
              disabled={desabilitado}
            />
          )
        }
      />
      <HelperText type={erro ? 'error' : 'info'} visible={Boolean(erro ?? dica)}>
        {erro ?? dica ?? ''}
      </HelperText>
    </View>
  );
}
