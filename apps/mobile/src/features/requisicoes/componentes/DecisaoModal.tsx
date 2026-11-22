import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import {
  Button,
  HelperText,
  Modal,
  Portal,
  SegmentedButtons,
  Text,
  TextInput,
} from 'react-native-paper';

import { CampoData } from '@/shared';
import { erroDoCampo, paraAppError } from '@/shared/api';
import { hoje } from '@/shared/utils/data';
import { useAppTheme } from '@/theme';

import type { Requisicao } from '../api';
import { useDecidirRequisicao } from '../hooks';
import {
  TAMANHO_MAXIMO_OBSERVACAO,
  temErros,
  validarDecisao,
  type ErrosDecisao,
} from '../validations';

type Props = { requisicao: Requisicao; visivel: boolean; aoFechar: () => void };

export function DecisaoModal({ requisicao, visivel, aoFechar }: Props) {
  const theme = useAppTheme();
  return (
    <Portal>
      <Modal
        visible={visivel}
        onDismiss={aoFechar}
        style={styles.modal}
        contentContainerStyle={[styles.folha, { backgroundColor: theme.colors.surface }]}
      >
        {visivel ? <Conteudo requisicao={requisicao} aoFechar={aoFechar} /> : null}
      </Modal>
    </Portal>
  );
}

function Conteudo({ requisicao, aoFechar }: Omit<Props, 'visivel'>) {
  const [decisao, setDecisao] = useState<'aprovar' | 'recusar'>('aprovar');
  const [previsao, setPrevisao] = useState<string | null>(null);
  const [observacao, setObservacao] = useState('');
  const [motivo, setMotivo] = useState('');
  const [erros, setErros] = useState<ErrosDecisao>({});
  const decidir = useDecidirRequisicao(requisicao);
  const erroDaApi = decidir.error ? paraAppError(decidir.error) : null;
  const erroDe = (campo: keyof ErrosDecisao) => erros[campo] ?? erroDoCampo(erroDaApi, campo);
  const erroGeral =
    erroDaApi && !['previsaoChegada', 'motivo'].includes(String(erroDaApi.extras.campo))
      ? erroDaApi.message
      : undefined;

  function confirmar() {
    if (decidir.isPending) {
      return;
    }
    const encontrados = validarDecisao({
      decisao,
      previsaoChegada: previsao,
      motivo,
      observacao,
      hoje: hoje(),
    });
    setErros(encontrados);
    if (temErros(encontrados)) {
      return;
    }
    decidir.mutate(
      decisao === 'aprovar'
        ? {
            tipo: 'aprovar',
            previsaoChegada: previsao ?? undefined,
            observacao: observacao.trim() || undefined,
          }
        : { tipo: 'recusar', motivo: motivo.trim() },
      { onSuccess: aoFechar },
    );
  }

  return (
    <View style={styles.conteudo}>
      <Text variant="titleLarge">Decidir {requisicao.numero}</Text>
      <Text variant="bodyMedium">Pedida por {requisicao.solicitanteNome}</Text>
      <SegmentedButtons
        value={decisao}
        onValueChange={(valor) => {
          setDecisao(valor as 'aprovar' | 'recusar');
          setErros({});
          decidir.reset();
        }}
        buttons={[
          { value: 'aprovar', label: 'Aprovar', icon: 'check', disabled: decidir.isPending },
          { value: 'recusar', label: 'Recusar', icon: 'close', disabled: decidir.isPending },
        ]}
      />
      {decisao === 'aprovar' ? (
        <>
          <CampoData
            rotulo="Previsão de chegada"
            valor={previsao}
            aoMudar={setPrevisao}
            minimo={hoje()}
            erro={erroDe('previsaoChegada')}
            dica="Opcional. Ajuda quem pediu a se programar."
            desabilitado={decidir.isPending}
            opcional
          />
          <View>
            <TextInput
              label="Recado para quem pediu (opcional)"
              accessibilityLabel="Recado para quem pediu"
              mode="outlined"
              value={observacao}
              onChangeText={setObservacao}
              maxLength={TAMANHO_MAXIMO_OBSERVACAO}
              multiline
              error={Boolean(erroDe('observacao'))}
              disabled={decidir.isPending}
            />
            <HelperText type="error" visible={Boolean(erroDe('observacao'))}>
              {erroDe('observacao') ?? ''}
            </HelperText>
          </View>
        </>
      ) : (
        <View>
          <TextInput
            label="Motivo da recusa"
            accessibilityLabel="Motivo da recusa"
            mode="outlined"
            value={motivo}
            onChangeText={setMotivo}
            maxLength={TAMANHO_MAXIMO_OBSERVACAO}
            multiline
            error={Boolean(erroDe('motivo'))}
            disabled={decidir.isPending}
          />
          <HelperText type="error" visible={Boolean(erroDe('motivo'))}>
            {erroDe('motivo') ?? ''}
          </HelperText>
        </View>
      )}
      {erroGeral ? (
        <HelperText type="error" visible>
          {erroGeral}
        </HelperText>
      ) : null}
      <View style={styles.botoes}>
        <Button onPress={aoFechar} disabled={decidir.isPending}>
          Voltar
        </Button>
        <Button
          mode="contained"
          onPress={confirmar}
          loading={decidir.isPending}
          disabled={decidir.isPending}
          contentStyle={styles.botao}
        >
          {decisao === 'aprovar' ? 'Confirmar aprovação' : 'Confirmar recusa'}
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  modal: { justifyContent: 'flex-end', margin: 0 },
  folha: { borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  conteudo: { padding: 24, gap: 12 },
  botoes: { flexDirection: 'row', justifyContent: 'flex-end', gap: 8 },
  botao: { minHeight: 48 },
});
