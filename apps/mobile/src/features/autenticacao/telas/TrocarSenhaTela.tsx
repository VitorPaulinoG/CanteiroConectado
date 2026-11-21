import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, HelperText, List, Text, TextInput } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { paraAppError } from '@/shared/api';
import { trocaDeSenhaPendente, useSessao, useUsuario } from '@/shared/sessao';
import { useAppTheme } from '@/theme';

import { postarTrocaDeSenha } from '../api';
import {
  regrasDaNovaSenha,
  temErros,
  validarTrocaDeSenha,
  type ErrosTrocaDeSenha,
} from '../validations';

type CampoSenhaProps = {
  rotulo: string;
  valor: string;
  aoMudar: (valor: string) => void;
  erro?: string;
  desabilitado: boolean;
};

function CampoSenha({ rotulo, valor, aoMudar, erro, desabilitado }: CampoSenhaProps) {
  const [mostrar, setMostrar] = useState(false);
  return (
    <View>
      <TextInput
        label={rotulo}
        accessibilityLabel={rotulo}
        mode="outlined"
        value={valor}
        onChangeText={aoMudar}
        secureTextEntry={!mostrar}
        autoCapitalize="none"
        error={Boolean(erro)}
        disabled={desabilitado}
        right={
          <TextInput.Icon
            icon={mostrar ? 'eye-off' : 'eye'}
            accessibilityLabel={`${mostrar ? 'Esconder' : 'Mostrar'} ${rotulo.toLowerCase()}`}
            onPress={() => setMostrar((atual) => !atual)}
          />
        }
      />
      <HelperText type="error" visible={Boolean(erro)}>
        {erro ?? ''}
      </HelperText>
    </View>
  );
}

const CODIGOS_DA_NOVA_SENHA = new Set(['SENHA_FRACA', 'SENHA_IGUAL_A_ATUAL']);

export function TrocarSenhaTela() {
  const theme = useAppTheme();
  const router = useRouter();
  const sessao = useSessao();
  const usuario = useUsuario();
  const pendente = trocaDeSenhaPendente(sessao.estado);
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erros, setErros] = useState<ErrosTrocaDeSenha>({});

  const troca = useMutation({
    mutationFn: () => postarTrocaDeSenha(senhaAtual, novaSenha),
    onSuccess: (atualizado) => {
      sessao.atualizarUsuario(atualizado);
      router.replace('/inicio');
    },
  });

  const erroDaApi = troca.error ? paraAppError(troca.error) : null;
  const errosExibidos: ErrosTrocaDeSenha = {
    ...erros,
    ...(erroDaApi?.codigo === 'SENHA_ATUAL_INCORRETA' ? { senhaAtual: erroDaApi.message } : {}),
    ...(erroDaApi && CODIGOS_DA_NOVA_SENHA.has(erroDaApi.codigo)
      ? { novaSenha: erroDaApi.message }
      : {}),
  };
  const erroGeral =
    erroDaApi &&
    erroDaApi.codigo !== 'SENHA_ATUAL_INCORRETA' &&
    !CODIGOS_DA_NOVA_SENHA.has(erroDaApi.codigo)
      ? erroDaApi.message
      : null;

  function mudar(setter: (valor: string) => void) {
    return (valor: string) => {
      setter(valor);
      troca.reset();
    };
  }

  function salvar() {
    const encontrados = validarTrocaDeSenha(senhaAtual, novaSenha, confirmacao);
    setErros(encontrados);
    if (!temErros(encontrados) && !troca.isPending) {
      troca.mutate();
    }
  }

  function trocarDepois() {
    sessao.adiarTroca();
    router.replace('/inicio');
  }

  return (
    <SafeAreaView style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <KeyboardAvoidingView
        style={styles.tela}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
          <Text variant="headlineSmall" accessibilityRole="header">
            {pendente ? 'Crie a sua senha' : 'Trocar senha'}
          </Text>
          <Text variant="bodyLarge" style={styles.explicacao}>
            {pendente && !usuario.administrador
              ? 'Para continuar, troque a senha temporária por uma que só você conheça.'
              : 'Escolha uma senha nova que só você conheça.'}
          </Text>

          <CampoSenha
            rotulo="Senha atual"
            valor={senhaAtual}
            aoMudar={mudar(setSenhaAtual)}
            erro={errosExibidos.senhaAtual}
            desabilitado={troca.isPending}
          />
          <CampoSenha
            rotulo="Nova senha"
            valor={novaSenha}
            aoMudar={mudar(setNovaSenha)}
            erro={errosExibidos.novaSenha}
            desabilitado={troca.isPending}
          />
          <CampoSenha
            rotulo="Confirmar nova senha"
            valor={confirmacao}
            aoMudar={mudar(setConfirmacao)}
            erro={errosExibidos.confirmacao}
            desabilitado={troca.isPending}
          />

          <List.Section>
            <List.Subheader>A nova senha precisa ter</List.Subheader>
            {regrasDaNovaSenha(senhaAtual, novaSenha, confirmacao).map((regra) => (
              <List.Item
                key={regra.descricao}
                title={regra.descricao}
                accessibilityLabel={`${regra.descricao}: ${regra.atendida ? 'atendida' : 'pendente'}`}
                left={(props) => (
                  <List.Icon
                    {...props}
                    icon={regra.atendida ? 'check-circle' : 'circle-outline'}
                    color={regra.atendida ? theme.status.normal : theme.colors.outline}
                  />
                )}
              />
            ))}
          </List.Section>

          {erroGeral ? (
            <HelperText type="error" visible>
              {erroGeral}
            </HelperText>
          ) : null}
        </ScrollView>

        <View style={styles.rodape}>
          <Button
            mode="contained"
            onPress={salvar}
            loading={troca.isPending}
            disabled={troca.isPending}
            contentStyle={styles.botao}
          >
            Salvar senha
          </Button>
          {pendente && usuario.administrador ? (
            <Button mode="text" onPress={trocarDepois} disabled={troca.isPending}>
              Trocar depois
            </Button>
          ) : null}
          {!pendente ? (
            <Button mode="text" onPress={() => router.back()} disabled={troca.isPending}>
              Cancelar
            </Button>
          ) : null}
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 24, gap: 4 },
  explicacao: { marginBottom: 16 },
  rodape: { padding: 16, gap: 8 },
  botao: { minHeight: 48 },
});
