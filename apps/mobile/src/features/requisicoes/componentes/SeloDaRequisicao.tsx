import { Chip } from 'react-native-paper';

import { useAppTheme } from '@/theme';

import type { StatusRequisicao } from '../api';
import { coresDoStatus, ICONE_DO_STATUS, ROTULO_DO_STATUS } from '../status';

export function SeloDaRequisicao({ status }: { status: StatusRequisicao }) {
  const theme = useAppTheme();
  const cores = coresDoStatus(status, theme);
  return (
    <Chip
      compact
      icon={ICONE_DO_STATUS[status]}
      style={{ backgroundColor: cores.fundo, alignSelf: 'flex-start' }}
      textStyle={{ color: cores.texto }}
    >
      {ROTULO_DO_STATUS[status]}
    </Chip>
  );
}
