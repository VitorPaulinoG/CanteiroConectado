import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import {
  Button,
  HelperText,
  List,
  Modal,
  Portal,
  Switch,
  Text,
  TextInput,
} from 'react-native-paper';

import { erroDoCampo, erroSemCampo, paraAppError } from '@/shared/api';
import { useAppTheme } from '@/theme';

import {
  atualizarColaborador,
  chavesColaboradores,
  criarColaborador,
  type Colaborador,
} from './api';
import { validarNomeColaborador } from './validations';

type Props = {
  visivel: boolean;
  canteiroId: string;
  colaborador?: Colaborador;
  nomeInicial?: string;
  aoSalvar: (colaborador: Colaborador) => void;
  aoFechar: () => void;
};

export function ColaboradorModal(props: Props) {
  const theme = useAppTheme();
  return (
    <Portal>
      <Modal
        visible={props.visivel}
        onDismiss={props.aoFechar}
        contentContainerStyle={[styles.modal, { backgroundColor: theme.colors.surface }]}
      >
        {props.visivel ? <Conteudo {...props} /> : null}
      </Modal>
    </Portal>
  );
}

function Conteudo({ canteiroId, colaborador, nomeInicial, aoSalvar, aoFechar }: Props) {
  const queryClient = useQueryClient();
  const [nome, setNome] = useState(colaborador?.nome ?? nomeInicial ?? '');
  const [ativo, setAtivo] = useState(colaborador?.ativo ?? true);
  const [erro, setErro] = useState<string | undefined>();

  const salvar = useMutation({
    mutationFn: () =>
      colaborador
        ? atualizarColaborador(colaborador.id, { nome: nome.trim(), ativo })
        : criarColaborador(nome.trim(), canteiroId),
    onSuccess: async (salvo) => {
      await queryClient.invalidateQueries({ queryKey: chavesColaboradores.todos });
      aoSalvar(salvo);
    },
  });

  const erroDaApi = salvar.error ? paraAppError(salvar.error) : null;
  const erroNome = erro ?? erroDoCampo(erroDaApi, 'nome');
  const erroGeral = erroSemCampo(erroDaApi, ['nome']);

  function confirmar() {
    const problema = validarNomeColaborador(nome);
    setErro(problema);
    if (!problema && !salvar.isPending) {
      salvar.mutate();
    }
  }

  return (
    <View style={styles.conteudo}>
      <Text variant="titleLarge">{colaborador ? 'Editar colaborador' : 'Novo colaborador'}</Text>
      <View>
        <TextInput
          label="Nome"
          accessibilityLabel="Nome do colaborador"
          mode="outlined"
          value={nome}
          autoFocus
          onChangeText={(texto) => {
            setNome(texto);
            salvar.reset();
          }}
          onSubmitEditing={confirmar}
          error={Boolean(erroNome)}
          disabled={salvar.isPending}
        />
        <HelperText type="error" visible={Boolean(erroNome)}>
          {erroNome ?? ''}
        </HelperText>
      </View>
      {colaborador ? (
        <List.Item
          title="Ativo"
          description={ativo ? 'Aparece no seletor da saída.' : 'Não aparece no seletor da saída.'}
          right={() => <Switch value={ativo} onValueChange={setAtivo} accessibilityLabel="Ativo" />}
        />
      ) : null}
      {erroGeral ? (
        <HelperText type="error" visible>
          {erroGeral}
        </HelperText>
      ) : null}
      <View style={styles.botoes}>
        <Button onPress={aoFechar} disabled={salvar.isPending}>
          Cancelar
        </Button>
        <Button
          mode="contained"
          onPress={confirmar}
          loading={salvar.isPending}
          disabled={salvar.isPending}
        >
          Salvar
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  modal: { margin: 24, borderRadius: 16 },
  conteudo: { padding: 24, gap: 8 },
  botoes: { flexDirection: 'row', justifyContent: 'flex-end', gap: 8 },
});
