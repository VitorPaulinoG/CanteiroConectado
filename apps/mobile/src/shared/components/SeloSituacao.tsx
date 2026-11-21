import { Chip } from 'react-native-paper';

import { useAppTheme } from '@/theme';

type Props = { ativo: boolean };

export function SeloSituacao({ ativo }: Props) {
  const theme = useAppTheme();
  return (
    <Chip
      compact
      icon={ativo ? 'check-circle-outline' : 'pause-circle-outline'}
      style={{
        backgroundColor: ativo ? theme.status.normalContainer : theme.colors.surfaceVariant,
        alignSelf: 'center',
      }}
      textStyle={{ color: ativo ? theme.status.onNormalContainer : theme.colors.onSurfaceVariant }}
    >
      {ativo ? 'Ativo' : 'Inativo'}
    </Chip>
  );
}
