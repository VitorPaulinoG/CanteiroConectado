import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, HelperText, SegmentedButtons, Text, TextInput } from 'react-native-paper';

import { AcessoNegado, CampoQuantidade, TelaComCabecalho } from '@/shared';
import { erroDoCampo, erroSemCampo } from '@/shared/api';
import { useCanteiroAtivo, useCanteiroIdAtivo } from '@/shared/canteiro';
import { SeletorColaborador, type Colaborador } from '@/shared/colaborador';
import { SeletorItem, type ItemSelecionado } from '@/shared/item';
import { SomentePerfil } from '@/shared/sessao';
import { lerQuantidade } from '@/shared/utils/quantidade';
import { gerarId } from '@/shared/utils/uuid';

import { Comprovante, type ResumoDoPedido } from '../componentes/Comprovante';
import { useRegistrarMovimentacao } from '../hooks';
import {
  TAMANHO_MAXIMO_OBSERVACAO,
  temErros,
  textoOpcional,
  validarEntrada,
  type ErrosMovimentacao,
} from '../validations';

type Motivo = 'RECEBIMENTO' | 'DEVOLUCAO' | 'AJUSTE';

const CAMPOS = ['itemId', 'quantidade', 'colaboradorId', 'observacao', 'motivo'];

export function EntradaTela() {
  const router = useRouter();
  return (
    <TelaComCabecalho titulo="Registrar entrada" aoVoltar={() => router.back()}>
      <SomentePerfil perfis={['ALMOXARIFE']} alternativa={<AcessoNegado />}>
        <Formulario />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}

function Formulario() {
  const router = useRouter();
  const canteiroId = useCanteiroIdAtivo();
  const { canteiro } = useCanteiroAtivo();
  const [id] = useState(gerarId);
  const [item, setItem] = useState<ItemSelecionado | null>(null);
  const [quantidade, setQuantidade] = useState('1');
  const [motivo, setMotivo] = useState<Motivo>('RECEBIMENTO');
  const [colaborador, setColaborador] = useState<Colaborador | null>(null);
  const [observacao, setObservacao] = useState('');
  const [erros, setErros] = useState<ErrosMovimentacao>({});
  const [resumo, setResumo] = useState<ResumoDoPedido | null>(null);

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
    const devolucao = motivo === 'DEVOLUCAO';
    const encontrados = validarEntrada({
      item,
      quantidade,
      motivo,
      colaboradorId: devolucao ? (colaborador?.id ?? null) : null,
      observacao,
    });
    setErros(encontrados);
    if (temErros(encontrados) || !item) {
      return;
    }
    const pedido = {
      id,
      tipo: 'ENTRADA' as const,
      motivo,
      itemId: item.itemId,
      quantidade: lerQuantidade(quantidade) as number,
      colaboradorId: devolucao ? colaborador?.id : undefined,
      observacao: textoOpcional(observacao),
    };
    setResumo({
      titulo: 'Comprovante de entrada',
      itemDescricao: item.descricao,
      itemCodigo: item.codigo,
      unidadeSimbolo: item.unidadeSimbolo,
      motivo,
      quantidade: pedido.quantidade,
      colaboradorNome: devolucao ? colaborador?.nome : undefined,
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
        aoNovoRegistro={() => router.replace('/movimentar/entrada')}
        aoConcluir={() => router.back()}
        rotuloNovoRegistro="Nova entrada"
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
          valor={item}
          aoMudar={mudar(setItem)}
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
            { value: 'RECEBIMENTO', label: 'Recebimento', disabled: enviando },
            { value: 'DEVOLUCAO', label: 'Devolução', disabled: enviando },
            { value: 'AJUSTE', label: 'Ajuste', disabled: enviando },
          ]}
          style={styles.motivo}
        />
        {motivo === 'DEVOLUCAO' ? (
          <SeletorColaborador
            canteiroId={canteiroId}
            valor={colaborador}
            aoMudar={mudar(setColaborador)}
            rotulo="Quem está devolvendo"
            erro={erroDe('colaboradorId')}
            desabilitado={enviando}
          />
        ) : null}
        <View>
          <TextInput
            label={motivo === 'AJUSTE' ? 'O que aconteceu' : 'Observação (nota fiscal, fornecedor)'}
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
          {enviando ? 'Enviando…' : 'Confirmar entrada'}
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
