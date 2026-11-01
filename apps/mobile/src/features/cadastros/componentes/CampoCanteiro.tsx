import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { View } from 'react-native';
import { HelperText, Menu, TextInput } from 'react-native-paper';

import { chavesCanteiros, listarCanteiros } from '@/shared/canteiro';

type Props = {
  valor: string | null;
  aoMudar: (canteiroId: string) => void;
  erro?: string;
  desabilitado?: boolean;
};

export function CampoCanteiro({ valor, aoMudar, erro, desabilitado }: Props) {
  const [aberto, setAberto] = useState(false);
  const canteiros = useQuery({
    queryKey: chavesCanteiros.lista(undefined),
    queryFn: () => listarCanteiros(),
  });
  const escolhido = canteiros.data?.find((canteiro) => canteiro.id === valor);
  const opcoes = canteiros.data?.filter((canteiro) => canteiro.ativo) ?? [];

  return (
    <View>
      <Menu
        visible={aberto}
        onDismiss={() => setAberto(false)}
        anchor={
          <TextInput
            label="Canteiro"
            accessibilityLabel="Canteiro"
            mode="outlined"
            value={escolhido?.nome ?? ''}
            editable={false}
            error={Boolean(erro)}
            disabled={desabilitado}
            right={
              <TextInput.Icon
                icon="menu-down"
                accessibilityLabel="Escolher canteiro"
                onPress={() => setAberto(true)}
                disabled={desabilitado}
              />
            }
            onPressIn={() => !desabilitado && setAberto(true)}
          />
        }
      >
        {opcoes.map((canteiro) => (
          <Menu.Item
            key={canteiro.id}
            title={canteiro.nome}
            onPress={() => {
              aoMudar(canteiro.id);
              setAberto(false);
            }}
          />
        ))}
        {canteiros.isSuccess && opcoes.length === 0 ? (
          <Menu.Item title="Nenhum canteiro ativo" disabled />
        ) : null}
      </Menu>
      <HelperText type="error" visible={Boolean(erro)}>
        {erro ?? ''}
      </HelperText>
    </View>
  );
}
