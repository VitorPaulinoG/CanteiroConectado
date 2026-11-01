import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, HelperText, SegmentedButtons, Text, TextInput } from 'react-native-paper';

import { AcessoNegado, ErrorState, LoadingState, SeloSituacao, TelaComCabecalho } from '@/shared';
import { erroDoCampo, erroSemCampo, paraAppError } from '@/shared/api';
import { SomentePerfil, useUsuario, type Perfil } from '@/shared/sessao';

import { CampoCanteiro } from '../componentes/CampoCanteiro';
import { ConfirmacaoDialog } from '../componentes/ConfirmacaoDialog';
import { CredencialInicialCard } from '../componentes/CredencialInicialCard';
import {
  ativarFuncionario,
  atualizarFuncionario,
  buscarFuncionario,
  chavesFuncionarios,
  criarFuncionario,
  desativarFuncionario,
  redefinirSenha,
  type Funcionario,
} from '../usuarios';
import { temErros, validarFuncionario, type ErrosFuncionario } from '../validations';

type Props = { id?: string };

export function FuncionarioFormTela({ id }: Props) {
  const router = useRouter();
  const consulta = useQuery({
    queryKey: chavesFuncionarios.detalhe(id ?? 'novo'),
    queryFn: () => buscarFuncionario(id as string),
    enabled: Boolean(id),
  });

  return (
    <TelaComCabecalho
      titulo={id ? 'Funcionário' : 'Novo funcionário'}
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
          <Formulario key={consulta.data.id} funcionario={consulta.data} />
        )}
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

type Confirmacao = 'redefinir' | 'desativar' | 'ativar' | null;

function Formulario({ funcionario }: { funcionario?: Funcionario }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const eu = useUsuario();
  const [nome, setNome] = useState(funcionario?.nome ?? '');
  const [email, setEmail] = useState(funcionario?.email ?? '');
  const [perfil, setPerfil] = useState<Perfil>(funcionario?.perfil ?? 'ALMOXARIFE');
  const [canteiroId, setCanteiroId] = useState<string | null>(funcionario?.canteiroId ?? null);
  const [erros, setErros] = useState<ErrosFuncionario>({});
  const [confirmacao, setConfirmacao] = useState<Confirmacao>(null);

  async function atualizarCache(salvo: Funcionario) {
    queryClient.setQueryData(chavesFuncionarios.detalhe(salvo.id), salvo);
    await queryClient.invalidateQueries({ queryKey: chavesFuncionarios.todos });
  }

  const salvar = useMutation({
    mutationFn: () => {
      const dados = {
        nome: nome.trim(),
        email: email.trim(),
        perfil,
        canteiroId: perfil === 'ALMOXARIFE' ? canteiroId : null,
      };
      return funcionario ? atualizarFuncionario(funcionario.id, dados) : criarFuncionario(dados);
    },
    onSuccess: async (salvo) => {
      await atualizarCache(salvo);
      if (funcionario) {
        router.back();
      } else {
        router.replace(`/cadastros/usuarios/${salvo.id}`);
      }
    },
  });

  const acao = useMutation({
    mutationFn: (tipo: Exclude<Confirmacao, null>) => {
      const id = (funcionario as Funcionario).id;
      if (tipo === 'redefinir') {
        return redefinirSenha(id);
      }
      return tipo === 'desativar' ? desativarFuncionario(id) : ativarFuncionario(id);
    },
    onSuccess: atualizarCache,
  });

  const erroDaApi = salvar.error ? paraAppError(salvar.error) : null;
  const erroNome = erros.nome ?? erroDoCampo(erroDaApi, 'nome');
  const erroEmail = erros.email ?? erroDoCampo(erroDaApi, 'email');
  const erroCanteiro = erros.canteiroId ?? erroDoCampo(erroDaApi, 'canteiroId');
  const erroGeral =
    erroSemCampo(erroDaApi, ['nome', 'email', 'canteiroId']) ??
    (acao.error ? paraAppError(acao.error).message : undefined);
  const ocupado = salvar.isPending || acao.isPending;
  const souEu = funcionario?.id === eu.id;

  function confirmar() {
    const encontrados = validarFuncionario({ nome, email, perfil, canteiroId });
    setErros(encontrados);
    if (!temErros(encontrados) && !ocupado) {
      salvar.mutate();
    }
  }

  function executar() {
    if (confirmacao) {
      acao.mutate(confirmacao);
    }
    setConfirmacao(null);
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
        {funcionario ? (
          <View style={styles.situacao}>
            <SeloSituacao ativo={funcionario.ativo} />
            {funcionario.administrador ? <Text variant="labelLarge">Administrador</Text> : null}
          </View>
        ) : null}

        {funcionario?.credencialInicialDisponivel ? (
          <CredencialInicialCard funcionarioId={funcionario.id} />
        ) : null}

        <View>
          <TextInput
            label="Nome"
            accessibilityLabel="Nome"
            mode="outlined"
            value={nome}
            onChangeText={setNome}
            error={Boolean(erroNome)}
            disabled={ocupado}
          />
          <HelperText type="error" visible={Boolean(erroNome)}>
            {erroNome ?? ''}
          </HelperText>
        </View>
        <View>
          <TextInput
            label="E-mail"
            accessibilityLabel="E-mail"
            mode="outlined"
            value={email}
            onChangeText={(texto) => {
              setEmail(texto);
              salvar.reset();
            }}
            autoCapitalize="none"
            keyboardType="email-address"
            error={Boolean(erroEmail)}
            disabled={ocupado}
          />
          <HelperText type="error" visible={Boolean(erroEmail)}>
            {erroEmail ?? ''}
          </HelperText>
        </View>

        <Text variant="labelLarge">Perfil</Text>
        <SegmentedButtons
          value={perfil}
          onValueChange={(valor) => setPerfil(valor as Perfil)}
          buttons={[
            {
              value: 'ALMOXARIFE',
              label: 'Almoxarife',
              disabled: ocupado || funcionario?.administrador,
            },
            { value: 'GESTOR', label: 'Gestor', disabled: ocupado || funcionario?.administrador },
          ]}
          style={styles.perfil}
        />

        {perfil === 'ALMOXARIFE' ? (
          <CampoCanteiro
            valor={canteiroId}
            aoMudar={setCanteiroId}
            erro={erroCanteiro}
            desabilitado={ocupado}
          />
        ) : null}

        {erroGeral ? (
          <HelperText type="error" visible>
            {erroGeral}
          </HelperText>
        ) : null}

        {funcionario ? (
          <View style={styles.acoes}>
            <Button
              mode="outlined"
              icon="lock-reset"
              onPress={() => setConfirmacao('redefinir')}
              disabled={ocupado}
            >
              Redefinir senha
            </Button>
            {funcionario.ativo ? (
              <Button
                mode="outlined"
                icon="account-off-outline"
                onPress={() => setConfirmacao('desativar')}
                disabled={ocupado || souEu}
              >
                Desativar
              </Button>
            ) : (
              <Button
                mode="outlined"
                icon="account-check-outline"
                onPress={() => setConfirmacao('ativar')}
                disabled={ocupado}
              >
                Reativar
              </Button>
            )}
          </View>
        ) : null}
      </ScrollView>

      <View style={styles.rodape}>
        <Button
          mode="contained"
          onPress={confirmar}
          loading={salvar.isPending}
          disabled={ocupado}
          contentStyle={styles.botao}
        >
          {funcionario ? 'Salvar' : 'Criar funcionário'}
        </Button>
      </View>

      <ConfirmacaoDialog
        visivel={confirmacao === 'redefinir'}
        titulo="Redefinir a senha?"
        mensagem="Uma nova senha temporária será gerada e o funcionário terá de trocá-la no próximo login."
        confirmar="Redefinir"
        aoConfirmar={executar}
        aoCancelar={() => setConfirmacao(null)}
      />
      <ConfirmacaoDialog
        visivel={confirmacao === 'desativar'}
        titulo="Desativar o funcionário?"
        mensagem="Ele deixa de entrar no app na hora. O histórico dele continua guardado."
        confirmar="Desativar"
        aoConfirmar={executar}
        aoCancelar={() => setConfirmacao(null)}
      />
      <ConfirmacaoDialog
        visivel={confirmacao === 'ativar'}
        titulo="Reativar o funcionário?"
        mensagem="Ele volta a entrar no app com a senha que já tinha."
        confirmar="Reativar"
        aoConfirmar={executar}
        aoCancelar={() => setConfirmacao(null)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 4 },
  situacao: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 12 },
  perfil: { marginTop: 8, marginBottom: 16 },
  acoes: { gap: 12, marginTop: 16 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
