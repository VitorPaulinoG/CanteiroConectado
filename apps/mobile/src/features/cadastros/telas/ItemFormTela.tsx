import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState, type ComponentProps } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import {
  Button,
  HelperText,
  List,
  SegmentedButtons,
  Switch,
  Text,
  TextInput,
} from 'react-native-paper';

import { AcessoNegado, ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { erroDoCampo, erroSemCampo, paraAppError } from '@/shared/api';
import { buscarItem, chavesItens, type Item, type TipoItem, type Unidade } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { formatarNumero, lerQuantidade } from '@/shared/utils/quantidade';

import { CampoUnidade } from '../componentes/CampoUnidade';
import { ConfirmacaoDialog } from '../componentes/ConfirmacaoDialog';
import { atualizarItem, criarItem } from '../itens';
import { temErros, validarItem, type ErrosItem } from '../validations';

type Props = { id?: string };

export function ItemFormTela({ id }: Props) {
  const router = useRouter();
  const consulta = useQuery({
    queryKey: chavesItens.detalhe(id ?? 'novo'),
    queryFn: () => buscarItem(id as string),
    enabled: Boolean(id),
  });

  return (
    <TelaComCabecalho titulo={id ? 'Editar item' : 'Novo item'} aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE', 'GESTOR']} alternativa={<AcessoNegado />}>
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
          <Formulario key={consulta.data.id} item={consulta.data} />
        )}
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

const CAMPOS = ['codigo', 'descricao', 'unidade', 'tipo', 'quantidadeMinima'];

function Formulario({ item }: { item?: Item }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [codigo, setCodigo] = useState(item?.codigo ?? '');
  const [descricao, setDescricao] = useState(item?.descricao ?? '');
  const [unidade, setUnidade] = useState<Unidade | null>(item?.unidade ?? null);
  const [tipo, setTipo] = useState<TipoItem | null>(item?.tipo ?? null);
  const [minimo, setMinimo] = useState(item ? formatarNumero(item.quantidadeMinima) : '');
  const [ativo, setAtivo] = useState(item?.ativo ?? true);
  const [erros, setErros] = useState<ErrosItem>({});
  const [confirmandoDesativacao, setConfirmandoDesativacao] = useState(false);

  const salvar = useMutation({
    mutationFn: () => {
      const dados = {
        codigo: codigo.trim(),
        descricao: descricao.trim(),
        unidade: unidade as Unidade,
        tipo: tipo as TipoItem,
        quantidadeMinima: lerQuantidade(minimo) ?? 0,
      };
      if (!item) {
        return criarItem(dados);
      }
      const mudou = {
        ...(dados.codigo !== item.codigo ? { codigo: dados.codigo } : {}),
        ...(dados.unidade !== item.unidade ? { unidade: dados.unidade } : {}),
      };
      return atualizarItem(item.id, {
        ...mudou,
        descricao: dados.descricao,
        tipo: dados.tipo,
        quantidadeMinima: dados.quantidadeMinima,
        ativo,
      });
    },
    onSuccess: async (salvo) => {
      queryClient.setQueryData(chavesItens.detalhe(salvo.id), salvo);
      await queryClient.invalidateQueries({ queryKey: chavesItens.todos });
      router.back();
    },
  });

  const erroDaApi = salvar.error ? paraAppError(salvar.error) : null;
  const erroDe = (campo: keyof ErrosItem) => erros[campo] ?? erroDoCampo(erroDaApi, campo);
  const erroGeral = erroSemCampo(erroDaApi, CAMPOS);

  function confirmar() {
    const encontrados = validarItem({ codigo, descricao, unidade, tipo, quantidadeMinima: minimo });
    setErros(encontrados);
    if (temErros(encontrados) || salvar.isPending) {
      return;
    }
    if (item?.ativo && !ativo) {
      setConfirmandoDesativacao(true);
      return;
    }
    salvar.mutate();
  }

  function campo(
    rotulo: string,
    valor: string,
    aoMudar: (texto: string) => void,
    erro: string | undefined,
    extra: Partial<ComponentProps<typeof TextInput>> = {},
  ) {
    return (
      <View>
        <TextInput
          label={rotulo}
          accessibilityLabel={rotulo}
          mode="outlined"
          value={valor}
          onChangeText={(texto) => {
            aoMudar(texto);
            salvar.reset();
          }}
          error={Boolean(erro)}
          disabled={salvar.isPending}
          {...extra}
        />
        <HelperText type="error" visible={Boolean(erro)}>
          {erro ?? ''}
        </HelperText>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
        {campo('Código', codigo, setCodigo, erroDe('codigo'), { autoCapitalize: 'characters' })}
        {campo('Descrição', descricao, setDescricao, erroDe('descricao'))}
        <CampoUnidade
          valor={unidade}
          aoMudar={(nova) => {
            setUnidade(nova);
            salvar.reset();
          }}
          erro={erroDe('unidade')}
          desabilitado={salvar.isPending}
        />
        <Text variant="labelLarge">Tipo</Text>
        <SegmentedButtons
          value={tipo ?? ''}
          onValueChange={(valor) => setTipo(valor as TipoItem)}
          buttons={[
            { value: 'MATERIAL', label: 'Material', disabled: salvar.isPending },
            { value: 'FERRAMENTA', label: 'Ferramenta', disabled: salvar.isPending },
          ]}
        />
        <HelperText type="error" visible={Boolean(erroDe('tipo'))}>
          {erroDe('tipo') ?? ''}
        </HelperText>
        {campo('Quantidade mínima', minimo, setMinimo, erroDe('quantidadeMinima'), {
          keyboardType: 'decimal-pad',
          placeholder: '0',
        })}
        <Text variant="bodyMedium" style={styles.dica}>
          Abaixo deste saldo o item aparece nos alertas. Zero significa sem alerta.
        </Text>
        {item ? (
          <List.Item
            title="Ativo"
            description={
              ativo
                ? 'Aparece nos seletores de movimentação.'
                : 'Some dos seletores, mas fica no histórico.'
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
      <ConfirmacaoDialog
        visivel={confirmandoDesativacao}
        titulo="Desativar o item?"
        mensagem="Ele some dos seletores de entrada, saída e requisição, mas continua no histórico."
        confirmar="Desativar"
        aoConfirmar={() => {
          setConfirmandoDesativacao(false);
          salvar.mutate();
        }}
        aoCancelar={() => setConfirmandoDesativacao(false)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 4 },
  dica: { marginTop: -4, marginBottom: 8 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
