import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Divider, HelperText, Text, TextInput } from 'react-native-paper';

import {
  AcessoNegado,
  CampoData,
  FaixaEstadoEnvio,
  LoadingState,
  TelaComCabecalho,
} from '@/shared';
import { useCanteiroAtivo, useCanteiroIdAtivo } from '@/shared/canteiro';
import { buscarDetalheEstoque, chavesEstoque } from '@/shared/estoque';
import { SeletorItemModal, type ItemSelecionado } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { hoje } from '@/shared/utils/data';
import { formatarData } from '@/shared/utils/format';
import { lerQuantidade } from '@/shared/utils/quantidade';
import { gerarId } from '@/shared/utils/uuid';

import { LinhaDeItem } from '../componentes/LinhaDeItem';
import { useCriarRequisicao } from '../hooks';
import {
  MAXIMO_DE_ITENS,
  TAMANHO_MAXIMO_FRENTE,
  TAMANHO_MAXIMO_OBSERVACAO,
  temErros,
  validarRequisicao,
  type ErrosRequisicao,
  type LinhaDaRequisicao,
} from '../validations';

type Props = { itemIdInicial?: string };

export function NovaRequisicaoTela({ itemIdInicial }: Props) {
  const router = useRouter();
  return (
    <TelaComCabecalho titulo="Nova requisição" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE', 'GESTOR']} alternativa={<AcessoNegado />}>
        <ComItemInicial itemIdInicial={itemIdInicial} />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

function ComItemInicial({ itemIdInicial }: Props) {
  const canteiroId = useCanteiroIdAtivo();
  const inicial = useQuery({
    queryKey: chavesEstoque.detalhe(canteiroId, itemIdInicial ?? ''),
    queryFn: () => buscarDetalheEstoque(canteiroId, itemIdInicial as string),
    enabled: Boolean(itemIdInicial),
  });

  if (itemIdInicial && inicial.isPending) {
    return <LoadingState message="Carregando o item..." />;
  }
  return <Formulario linhasIniciais={inicial.data ? [paraLinha(inicial.data)] : []} />;
}

function paraLinha(item: Omit<ItemSelecionado, 'status'>): LinhaDaRequisicao {
  return {
    itemId: item.itemId,
    codigo: item.codigo,
    descricao: item.descricao,
    unidadeSimbolo: item.unidadeSimbolo,
    saldo: item.saldo,
    quantidade: '',
  };
}

const CAMPOS_DO_FORMULARIO =
  /^(itens|necessarioAte|frenteObra|observacao|itens\[\d+\]\.(itemId|quantidade))$/;

function Formulario({ linhasIniciais }: { linhasIniciais: LinhaDaRequisicao[] }) {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const { canteiro } = useCanteiroAtivo();
  const [id] = useState(gerarId);
  const [linhas, setLinhas] = useState<LinhaDaRequisicao[]>(linhasIniciais);
  const [necessarioAte, setNecessarioAte] = useState<string | null>(null);
  const [frenteObra, setFrenteObra] = useState('');
  const [observacao, setObservacao] = useState('');
  const [erros, setErros] = useState<ErrosRequisicao>({});
  const [escolhendo, setEscolhendo] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);

  const envio = useCriarRequisicao(canteiroId);
  const enviando = envio.enviando;
  const erroDaApi = envio.falhou && !envio.semResposta ? envio.erro : null;
  const campoDaApi = typeof erroDaApi?.extras.campo === 'string' ? erroDaApi.extras.campo : null;
  const erroDe = (campo: string) =>
    erros[campo] ??
    (campoDaApi === campo
      ? erroDaApi?.message
      : campoDaApi?.replace('.itemId', '.quantidade') === campo && campoDaApi.endsWith('.itemId')
        ? erroDaApi?.message
        : undefined);
  const erroGeral =
    erroDaApi && (!campoDaApi || !CAMPOS_DO_FORMULARIO.test(campoDaApi))
      ? erroDaApi.message
      : undefined;

  function alterar(acao: () => void) {
    acao();
    envio.reiniciar();
  }

  function adicionar(item: ItemSelecionado) {
    setEscolhendo(false);
    if (linhas.some((linha) => linha.itemId === item.itemId)) {
      setAviso(`${item.descricao} já está na lista. Ajuste a quantidade na linha dele.`);
      return;
    }
    setAviso(null);
    alterar(() => setLinhas((atuais) => [...atuais, paraLinha(item)]));
  }

  function confirmar() {
    if (enviando) {
      return;
    }
    const encontrados = validarRequisicao({
      linhas,
      necessarioAte,
      frenteObra,
      observacao,
      hoje: hoje(),
    });
    setErros(encontrados);
    if (temErros(encontrados) || !necessarioAte) {
      return;
    }
    envio.enviar({
      id,
      canteiroId,
      necessarioAte,
      frenteObra: frenteObra.trim() || undefined,
      observacao: observacao.trim() || undefined,
      itens: linhas.map((linha) => ({
        itemId: linha.itemId,
        quantidade: lerQuantidade(linha.quantidade) as number,
      })),
    });
  }

  if (envio.salvo && envio.resposta) {
    const criada = envio.resposta;
    return (
      <View style={styles.tela}>
        <ScrollView contentContainerStyle={styles.conteudo}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Requisição {criada.numero}
          </Text>
          <FaixaEstadoEnvio estado="salvo" />
          <Text variant="bodyLarge">
            Pedido de {criada.itens.length} {criada.itens.length === 1 ? 'item' : 'itens'} para{' '}
            {formatarData(criada.necessarioAte)}. A outra pessoa decide e a resposta aparece em
            Requisições.
          </Text>
        </ScrollView>
        <View style={styles.rodape}>
          <Button
            mode="contained"
            onPress={() => router.replace(`/requisicoes/${criada.id}`)}
            contentStyle={styles.botao}
          >
            Ver requisição
          </Button>
          <Button mode="outlined" onPress={() => router.back()} contentStyle={styles.botao}>
            Concluir
          </Button>
        </View>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
        <Text variant="bodyMedium">Canteiro: {canteiro?.nome}</Text>
        {envio.enviando || (envio.falhou && envio.semResposta) ? (
          <FaixaEstadoEnvio
            estado={envio.estado}
            erro={envio.erro}
            aoTentarDeNovo={envio.tentarDeNovo}
          />
        ) : null}

        <Text variant="titleMedium">Itens</Text>
        {linhas.map((linha, indice) => (
          <View key={linha.itemId}>
            <LinhaDeItem
              linha={linha}
              aoMudarQuantidade={(quantidade) =>
                alterar(() =>
                  setLinhas((atuais) =>
                    atuais.map((atual, i) => (i === indice ? { ...atual, quantidade } : atual)),
                  ),
                )
              }
              aoRemover={() =>
                alterar(() => setLinhas((atuais) => atuais.filter((_, i) => i !== indice)))
              }
              erro={erroDe(`itens[${indice}].quantidade`)}
              desabilitado={enviando}
            />
            <Divider />
          </View>
        ))}
        <Button
          mode="outlined"
          icon="plus"
          onPress={() => setEscolhendo(true)}
          disabled={enviando || linhas.length >= MAXIMO_DE_ITENS}
          style={styles.adicionar}
        >
          Adicionar item
        </Button>
        <HelperText
          type={erroDe('itens') ? 'error' : 'info'}
          visible={Boolean(erroDe('itens') ?? aviso)}
        >
          {erroDe('itens') ?? aviso ?? ''}
        </HelperText>

        <CampoData
          rotulo="Necessário até"
          valor={necessarioAte}
          aoMudar={(valor) => alterar(() => setNecessarioAte(valor))}
          minimo={hoje()}
          erro={erroDe('necessarioAte')}
          desabilitado={enviando}
        />
        <View>
          <TextInput
            label="Frente de obra (opcional)"
            accessibilityLabel="Frente de obra"
            mode="outlined"
            value={frenteObra}
            onChangeText={(texto) => alterar(() => setFrenteObra(texto))}
            maxLength={TAMANHO_MAXIMO_FRENTE}
            error={Boolean(erroDe('frenteObra'))}
            disabled={enviando}
          />
          <HelperText type="error" visible={Boolean(erroDe('frenteObra'))}>
            {erroDe('frenteObra') ?? ''}
          </HelperText>
        </View>
        <View>
          <TextInput
            label="Observação (opcional)"
            accessibilityLabel="Observação"
            mode="outlined"
            value={observacao}
            onChangeText={(texto) => alterar(() => setObservacao(texto))}
            maxLength={TAMANHO_MAXIMO_OBSERVACAO}
            multiline
            error={Boolean(erroDe('observacao'))}
            disabled={enviando}
          />
          <HelperText type="error" visible={Boolean(erroDe('observacao'))}>
            {erroDe('observacao') ?? ''}
          </HelperText>
        </View>
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
          loading={enviando}
          disabled={enviando}
          contentStyle={styles.botao}
        >
          {enviando ? 'Enviando…' : 'Enviar requisição'}
        </Button>
      </View>
      <SeletorItemModal
        modo="estoque"
        canteiroId={canteiroId}
        visivel={escolhendo}
        aoEscolher={adicionar}
        aoFechar={() => setEscolhendo(false)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 8 },
  adicionar: { alignSelf: 'flex-start', marginTop: 4 },
  rodape: { padding: 16, gap: 8 },
  botao: { minHeight: 48 },
});
