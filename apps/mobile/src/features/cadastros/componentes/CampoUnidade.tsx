import { useState } from 'react';
import { View } from 'react-native';
import { HelperText, Menu, TextInput } from 'react-native-paper';

import { UNIDADES, type Unidade } from '@/shared/item';

type Props = {
  valor: Unidade | null;
  aoMudar: (unidade: Unidade) => void;
  erro?: string;
  desabilitado?: boolean;
};

export function CampoUnidade({ valor, aoMudar, erro, desabilitado }: Props) {
  const [aberto, setAberto] = useState(false);
  const escolhida = UNIDADES.find((unidade) => unidade.valor === valor);

  return (
    <View>
      <Menu
        visible={aberto}
        onDismiss={() => setAberto(false)}
        anchor={
          <TextInput
            label="Unidade"
            accessibilityLabel="Unidade"
            mode="outlined"
            value={escolhida?.simbolo ?? ''}
            editable={false}
            error={Boolean(erro)}
            disabled={desabilitado}
            onPressIn={() => !desabilitado && setAberto(true)}
            right={
              <TextInput.Icon
                icon="menu-down"
                accessibilityLabel="Escolher unidade"
                onPress={() => setAberto(true)}
                disabled={desabilitado}
              />
            }
          />
        }
      >
        {UNIDADES.map((unidade) => (
          <Menu.Item
            key={unidade.valor}
            title={unidade.simbolo}
            onPress={() => {
              aoMudar(unidade.valor);
              setAberto(false);
            }}
          />
        ))}
      </Menu>
      <HelperText type="error" visible={Boolean(erro)}>
        {erro ?? ''}
      </HelperText>
    </View>
  );
}
