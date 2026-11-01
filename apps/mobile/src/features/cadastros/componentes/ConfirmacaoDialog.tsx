import { Button, Dialog, Portal, Text } from 'react-native-paper';

type Props = {
  visivel: boolean;
  titulo: string;
  mensagem: string;
  confirmar: string;
  aoConfirmar: () => void;
  aoCancelar: () => void;
};

export function ConfirmacaoDialog({
  visivel,
  titulo,
  mensagem,
  confirmar,
  aoConfirmar,
  aoCancelar,
}: Props) {
  return (
    <Portal>
      <Dialog visible={visivel} onDismiss={aoCancelar}>
        <Dialog.Title>{titulo}</Dialog.Title>
        <Dialog.Content>
          <Text variant="bodyLarge">{mensagem}</Text>
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={aoCancelar}>Cancelar</Button>
          <Button onPress={aoConfirmar}>{confirmar}</Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}
