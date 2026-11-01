import { SegmentedButtons } from 'react-native-paper';

export type Situacao = 'ativos' | 'inativos' | 'todos';

export function situacaoParaAtivo(situacao: Situacao): boolean | undefined {
  if (situacao === 'todos') {
    return undefined;
  }
  return situacao === 'ativos';
}

type Props = { valor: Situacao; aoMudar: (valor: Situacao) => void };

export function FiltroSituacao({ valor, aoMudar }: Props) {
  return (
    <SegmentedButtons
      value={valor}
      onValueChange={(novo) => aoMudar(novo as Situacao)}
      buttons={[
        { value: 'ativos', label: 'Ativos' },
        { value: 'inativos', label: 'Inativos' },
        { value: 'todos', label: 'Todos' },
      ]}
    />
  );
}
