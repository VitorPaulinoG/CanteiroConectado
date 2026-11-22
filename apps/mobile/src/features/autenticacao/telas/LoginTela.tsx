import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Avatar, Banner, Button, HelperText, Text, TextInput } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { paraAppError, type AppError } from '@/shared/api';
import { useSessao } from '@/shared/sessao';
import { useAppTheme } from '@/theme';

import { minutosRestantes, temErros, validarLogin, type ErrosLogin } from '../validations';

function mensagemDoErro(erro: AppError): string {
  if (erro.codigo === 'CONTA_BLOQUEADA' && typeof erro.extras.bloqueadoAte === 'string') {
    const minutos = minutosRestantes(erro.extras.bloqueadoAte);
    return `Conta bloqueada por excesso de tentativas. Tente de novo em ${minutos} ${
      minutos === 1 ? 'minuto' : 'minutos'
    }.`;
  }
  return erro.message;
}

export function LoginTela() {
  const theme = useAppTheme();
  const { estado, entrar } = useSessao();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [erros, setErros] = useState<ErrosLogin>({});

  const login = useMutation({
    mutationFn: () => entrar(email, senha),
  });

  const aviso = estado.status === 'anonimo' ? estado.aviso : undefined;
  const erroDaApi = login.error ? paraAppError(login.error) : null;
  const erroNaSenha = erroDaApi?.codigo === 'CREDENCIAIS_INVALIDAS';

  function confirmar() {
    const encontrados = validarLogin(email, senha);
    setErros(encontrados);
    if (!temErros(encontrados) && !login.isPending) {
      login.mutate();
    }
  }

  return (
    <SafeAreaView style={[styles.tela, { backgroundColor: theme.colors.background }]}>
      <KeyboardAvoidingView
        style={styles.tela}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <Banner visible={Boolean(aviso) && !login.isPending && !login.isSuccess} icon="lock-clock">
          {aviso ?? ''}
        </Banner>
        <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
          <View style={styles.cabecalho}>
            <Avatar.Icon icon="hard-hat" size={80} />
            <Text variant="headlineSmall" accessibilityRole="header">
              Canteiro Conectado
            </Text>
            <Text variant="bodyLarge">
              Entre com o e-mail e a senha recebidos do administrador.
            </Text>
          </View>

          <View>
            <TextInput
              label="E-mail"
              accessibilityLabel="E-mail"
              mode="outlined"
              value={email}
              onChangeText={(texto) => {
                setEmail(texto);
                login.reset();
              }}
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              textContentType="emailAddress"
              error={Boolean(erros.email)}
              disabled={login.isPending}
            />
            <HelperText type="error" visible={Boolean(erros.email)}>
              {erros.email}
            </HelperText>
          </View>

          <View>
            <TextInput
              label="Senha"
              accessibilityLabel="Senha"
              mode="outlined"
              value={senha}
              onChangeText={(texto) => {
                setSenha(texto);
                login.reset();
              }}
              secureTextEntry={!mostrarSenha}
              autoCapitalize="none"
              autoComplete="password"
              textContentType="password"
              error={Boolean(erros.senha) || erroNaSenha}
              disabled={login.isPending}
              onSubmitEditing={confirmar}
              right={
                <TextInput.Icon
                  icon={mostrarSenha ? 'eye-off' : 'eye'}
                  accessibilityLabel={mostrarSenha ? 'Esconder senha' : 'Mostrar senha'}
                  onPress={() => setMostrarSenha((atual) => !atual)}
                />
              }
            />
            <HelperText type="error" visible={Boolean(erros.senha) || erroNaSenha}>
              {erros.senha ?? (erroNaSenha ? erroDaApi?.message : '')}
            </HelperText>
          </View>

          {erroDaApi && !erroNaSenha ? (
            <HelperText type="error" visible accessibilityLiveRegion="polite">
              {mensagemDoErro(erroDaApi)}
            </HelperText>
          ) : null}
        </ScrollView>

        <View style={styles.rodape}>
          <Button
            mode="contained"
            onPress={confirmar}
            loading={login.isPending}
            disabled={login.isPending}
            contentStyle={styles.botao}
          >
            Entrar
          </Button>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 24, gap: 4, flexGrow: 1, justifyContent: 'center' },
  cabecalho: { alignItems: 'center', gap: 12, marginBottom: 24 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
