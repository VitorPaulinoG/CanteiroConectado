import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { Button, Dialog, HelperText, Portal, Text, TextInput } from 'react-native-paper';

import { FaixaEstadoEnvio } from '@/shared';
import type { Movimentacao } from '@/shared/estoque';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { gerarId } from '@/shared/utils/uuid';

import { useEstornar } from '../hooks';
import { TAMANHO_MAXIMO_OBSERVACAO, validarMotivoDoEstorno } from '../validations';

type Props = {
  movimentacao: Movimentacao;
  visivel: boolean;
  aoEstornar: () => void;
  aoFechar: () => void;
};

export function EstornoDialog(props: Props) {
  return (
    <Portal>
      <Dialog visible={props.visivel} onDismiss={props.aoFechar}>
        {props.visivel ? <Conteudo {...props} /> : null}
      </Dialog>
    </Portal>
  );
}

function Conteudo({ movimentacao, aoEstornar, aoFechar }: Props) {
  const [id] = useState(gerarId);
  const [motivo, setMotivo] = useState('');
  const [erro, setErro] = useState<string | undefined>();
  const envio = useEstornar(movimentacao);
  const contrario = movimentacao.tipo === 'ENTRADA' ? 'saída' : 'entrada';
  const erroDaApi = envio.falhou && !envio.semResposta ? envio.erro?.message : undefined;

  function confirmar() {
    if (envio.enviando) {
      return;
    }
    const problema = validarMotivoDoEstorno(motivo);
    setErro(problema);
    if (!problema) {
      envio.enviar({ id, motivo: motivo.trim() });
    }
  }

  if (envio.salvo) {
    return (
      <>
        <Dialog.Title>Estorno registrado</Dialog.Title>
        <Dialog.Content style={styles.conteudo}>
          <FaixaEstadoEnvio estado="salvo" />
          <Text variant="bodyLarge">
            O saldo foi corrigido. O lançamento original continua no histórico, ligado ao estorno.
          </Text>
        </Dialog.Content>
        <Dialog.Actions>
          <Button
            onPress={() => {
              aoEstornar();
              aoFechar();
            }}
          >
            Fechar
          </Button>
        </Dialog.Actions>
      </>
    );
  }

  return (
    <>
      <Dialog.Title>Estornar lançamento?</Dialog.Title>
      <Dialog.Content style={styles.conteudo}>
        <Text variant="bodyLarge">
          Será registrada uma {contrario} de{' '}
          {formatarQuantidade(movimentacao.quantidade, movimentacao.unidadeSimbolo)} de{' '}
          {movimentacao.itemDescricao} para desfazer este lançamento.
        </Text>
        <TextInput
          label="Motivo do estorno"
          accessibilityLabel="Motivo do estorno"
          mode="outlined"
          value={motivo}
          onChangeText={(texto) => {
            setMotivo(texto);
            envio.reiniciar();
          }}
          maxLength={TAMANHO_MAXIMO_OBSERVACAO}
          multiline
          error={Boolean(erro ?? erroDaApi)}
          disabled={envio.enviando}
        />
        <HelperText type="error" visible={Boolean(erro ?? erroDaApi)}>
          {erro ?? erroDaApi ?? ''}
        </HelperText>
        {envio.enviando || (envio.falhou && envio.semResposta) ? (
          <FaixaEstadoEnvio
            estado={envio.estado}
            erro={envio.erro}
            aoTentarDeNovo={envio.tentarDeNovo}
          />
        ) : null}
      </Dialog.Content>
      <Dialog.Actions>
        <Button onPress={aoFechar} disabled={envio.enviando}>
          Voltar
        </Button>
        <Button onPress={confirmar} loading={envio.enviando} disabled={envio.enviando}>
          Estornar
        </Button>
      </Dialog.Actions>
    </>
  );
}

const styles = StyleSheet.create({
  conteudo: { gap: 8 },
});
