import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, HelperText, SegmentedButtons, Text, TextInput } from 'react-native-paper';

import { AcessoNegado, CampoQuantidade, TelaComCabecalho } from '@/shared';
import { erroDoCampo, erroSemCampo } from '@/shared/api';
import { useCanteiroAtivo, useCanteiroIdAtivo } from '@/shared/canteiro';
import { SeletorColaborador, type Colaborador } from '@/shared/colaborador';
import { buscarDetalheEstoque, chavesEstoque, type DetalheEstoque } from '@/shared/estoque';
import { SeletorItem, type ItemSelecionado } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { lerQuantidade } from '@/shared/utils/quantidade';
import { gerarId } from '@/shared/utils/uuid';

import { Comprovante, type ResumoDoPedido } from '../componentes/Comprovante';
import { useRegistrarMovimentacao } from '../hooks';
import {
  TAMANHO_MAXIMO_FRENTE,
  TAMANHO_MAXIMO_OBSERVACAO,
  temErros,
  textoOpcional,
  validarSaida,
  type ErrosMovimentacao,
} from '../validations';

type Motivo = 'RETIRADA' | 'AJUSTE';

const CAMPOS = ['itemId', 'quantidade', 'colaboradorId', 'frenteObra', 'observacao', 'motivo'];

function doDetalhe(detalhe: DetalheEstoque): ItemSelecionado {
  return {
    itemId: detalhe.itemId,
    codigo: detalhe.codigo,
    descricao: detalhe.descricao,
    unidadeSimbolo: detalhe.unidadeSimbolo,
    tipo: detalhe.tipo,
    saldo: detalhe.saldo,
    status: detalhe.status,
  };
}

type Props = { itemIdInicial?: string };

export function SaidaTela({ itemIdInicial }: Props) {
  const router = useRouter();
  return (
    <TelaComCabecalho titulo="Registrar saída" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE']} alternativa={<AcessoNegado />}>
        <Formulario itemIdInicial={itemIdInicial} />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

function Formulario({ itemIdInicial }: Props) {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const { canteiro } = useCanteiroAtivo();
  const [id] = useState(gerarId);
  const [itemEscolhido, setItemEscolhido] = useState<ItemSelecionado | null | undefined>(undefined);
  const [quantidade, setQuantidade] = useState('1');
  const [motivo, setMotivo] = useState<Motivo>('RETIRADA');
  const [colaborador, setColaborador] = useState<Colaborador | null>(null);
  const [frenteObra, setFrenteObra] = useState('');
  const [observacao, setObservacao] = useState('');
  const [erros, setErros] = useState<ErrosMovimentacao>({});
  const [resumo, setResumo] = useState<ResumoDoPedido | null>(null);

  const inicial = useQuery({
    queryKey: chavesEstoque.detalhe(canteiroId, itemIdInicial ?? ''),
    queryFn: () => buscarDetalheEstoque(canteiroId, itemIdInicial as string),
    enabled: Boolean(itemIdInicial),
  });
  const item =
    itemEscolhido !== undefined ? itemEscolhido : inicial.data ? doDetalhe(inicial.data) : null;

  const envio = useRegistrarMovimentacao(canteiroId);
  const erroDaApi = envio.falhou && !envio.semResposta ? envio.erro : null;
  const erroDe = (campo: keyof ErrosMovimentacao) => erros[campo] ?? erroDoCampo(erroDaApi, campo);
  const erroGeral = erroSemCampo(erroDaApi, CAMPOS) ?? erroDoCampo(erroDaApi, 'motivo');
  const enviando = envio.enviando;

  function mudar<T>(setter: (valor: T) => void) {
    return (valor: T) => {
      setter(valor);
      envio.reiniciar();
    };
  }

  function confirmar() {
    if (enviando) {
      return;
    }
    const encontrados = validarSaida({
      item,
      quantidade,
      motivo,
      colaboradorId: colaborador?.id ?? null,
      frenteObra,
      observacao,
    });
    setErros(encontrados);
    if (temErros(encontrados) || !item) {
      return;
    }
    const pedido = {
      id,
      tipo: 'SAIDA' as const,
      motivo,
      itemId: item.itemId,
      quantidade: lerQuantidade(quantidade) as number,
      colaboradorId: colaborador?.id,
      frenteObra: textoOpcional(frenteObra),
      observacao: textoOpcional(observacao),
    };
    setResumo({
      titulo: 'Comprovante de saída',
      itemDescricao: item.descricao,
      itemCodigo: item.codigo,
      unidadeSimbolo: item.unidadeSimbolo,
      motivo,
      quantidade: pedido.quantidade,
      colaboradorNome: colaborador?.nome,
      frenteObra: pedido.frenteObra,
      observacao: pedido.observacao,
    });
    envio.enviar(pedido);
  }

  if (resumo && !(envio.falhou && !envio.semResposta)) {
    return (
      <Comprovante
        resumo={resumo}
        envio={envio}
        aoCorrigir={() => setResumo(null)}
        aoNovoRegistro={() => router.replace('/movimentar/saida')}
        aoConcluir={() => router.back()}
        rotuloNovoRegistro="Nova saída"
      />
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.conteudo} keyboardShouldPersistTaps="handled">
        <Text variant="bodyMedium">Canteiro: {canteiro?.nome}</Text>
        <SeletorItem
          modo="estoque"
          canteiroId={canteiroId}
          exigirSaldo
          valor={item}
          aoMudar={mudar(setItemEscolhido)}
          erro={erroDe('itemId')}
          desabilitado={enviando}
        />
        <CampoQuantidade
          valor={quantidade}
          aoMudar={mudar(setQuantidade)}
          unidadeSimbolo={item?.unidadeSimbolo}
          erro={erroDe('quantidade')}
          desabilitado={enviando}
        />
        <Text variant="labelLarge">Motivo</Text>
        <SegmentedButtons
          value={motivo}
          onValueChange={(valor) => mudar(setMotivo)(valor as Motivo)}
          buttons={[
            { value: 'RETIRADA', label: 'Retirada', disabled: enviando },
            { value: 'AJUSTE', label: 'Ajuste (perda, dano)', disabled: enviando },
          ]}
          style={styles.motivo}
        />
        <SeletorColaborador
          canteiroId={canteiroId}
          valor={colaborador}
          aoMudar={mudar(setColaborador)}
          rotulo={motivo === 'RETIRADA' ? 'Quem está retirando' : 'Colaborador (opcional)'}
          erro={erroDe('colaboradorId')}
          desabilitado={enviando}
        />
        <View>
          <TextInput
            label={motivo === 'RETIRADA' ? 'Frente de obra' : 'Frente de obra (opcional)'}
            accessibilityLabel="Frente de obra"
            mode="outlined"
            value={frenteObra}
            onChangeText={mudar(setFrenteObra)}
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
            label={motivo === 'AJUSTE' ? 'O que aconteceu' : 'Observação (opcional)'}
            accessibilityLabel="Observação"
            mode="outlined"
            value={observacao}
            onChangeText={mudar(setObservacao)}
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
          {enviando ? 'Enviando…' : 'Confirmar saída'}
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 4 },
  motivo: { marginTop: 8, marginBottom: 16 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
