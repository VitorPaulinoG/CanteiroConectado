import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, HelperText, List, Switch, TextInput } from 'react-native-paper';

import { AcessoNegado, ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { erroDoCampo, erroSemCampo, paraAppError } from '@/shared/api';
import { buscarCanteiro, chavesCanteiros, type Canteiro } from '@/shared/canteiro';
import { SomentePerfil } from '@/shared/sessao';

import { atualizarCanteiro, criarCanteiro } from '../api';
import { temErros, validarCanteiro, type ErrosCanteiro } from '../validations';

type Props = { id?: string };

export function CanteiroFormTela({ id }: Props) {
  const router = useRouter();
  const consulta = useQuery({
    queryKey: chavesCanteiros.detalhe(id ?? 'novo'),
    queryFn: () => buscarCanteiro(id as string),
    enabled: Boolean(id),
  });

  return (
    <TelaComCabecalho
      titulo={id ? 'Editar canteiro' : 'Novo canteiro'}
      aoVoltar={() => router.back()}
    >
      <SomentePerfil administrador alternativa={<AcessoNegado />}>
        {!id ? (
          <Formulario />
        ) : consulta.isPending ? (
          <LoadingState />
        ) : consulta.isError ? (
          <ErrorState
            message={paraAppError(consulta.error).message}
            onRetry={() => consulta.refetch()}
          />
        ) : (
          <Formulario canteiro={consulta.data} />
        )}
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

function Formulario({ canteiro }: { canteiro?: Canteiro }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [nome, setNome] = useState(canteiro?.nome ?? '');
  const [endereco, setEndereco] = useState(canteiro?.endereco ?? '');
  const [ativo, setAtivo] = useState(canteiro?.ativo ?? true);
  const [erros, setErros] = useState<ErrosCanteiro>({});

  const salvar = useMutation({
    mutationFn: () => {
      const dados = { nome: nome.trim(), endereco: endereco.trim() };
      return canteiro ? atualizarCanteiro(canteiro.id, { ...dados, ativo }) : criarCanteiro(dados);
    },
    onSuccess: async (salvo) => {
      queryClient.setQueryData(chavesCanteiros.detalhe(salvo.id), salvo);
      await queryClient.invalidateQueries({ queryKey: chavesCanteiros.todos });
      router.back();
    },
  });

  const erroDaApi = salvar.error ? paraAppError(salvar.error) : null;
  const erroNome = erros.nome ?? erroDoCampo(erroDaApi, 'nome');
  const erroGeral = erroSemCampo(erroDaApi, ['nome']);

  function confirmar() {
    const encontrados = validarCanteiro(nome, endereco);
    setErros(encontrados);
    if (!temErros(encontrados) && !salvar.isPending) {
      salvar.mutate();
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
        <View>
          <TextInput
            label="Nome"
            accessibilityLabel="Nome"
            mode="outlined"
            value={nome}
            onChangeText={(texto) => {
              setNome(texto);
              salvar.reset();
            }}
            error={Boolean(erroNome)}
            disabled={salvar.isPending}
          />
          <HelperText type="error" visible={Boolean(erroNome)}>
            {erroNome ?? ''}
          </HelperText>
        </View>
        <View>
          <TextInput
            label="Endereço"
            accessibilityLabel="Endereço"
            mode="outlined"
            value={endereco}
            onChangeText={setEndereco}
            multiline
            error={Boolean(erros.endereco)}
            disabled={salvar.isPending}
          />
          <HelperText type="error" visible={Boolean(erros.endereco)}>
            {erros.endereco ?? ''}
          </HelperText>
        </View>
        {canteiro ? (
          <List.Item
            title="Ativo"
            description={
              ativo
                ? 'O canteiro aparece na escolha do canteiro ativo.'
                : 'Desativado: não recebe movimentações.'
            }
            right={() => (
              <Switch
                value={ativo}
                onValueChange={setAtivo}
                disabled={salvar.isPending}
                accessibilityLabel="Ativo"
              />
            )}
          />
        ) : null}
        {erroGeral ? (
          <HelperText type="error" visible>
            {erroGeral}
          </HelperText>
        ) : null}
      </ScrollView>
      <View style={styles.rodape}>
        <Button
          mode="contained"
          onPress={confirmar}
          loading={salvar.isPending}
          disabled={salvar.isPending}
          contentStyle={styles.botao}
        >
          Salvar
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 4 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
