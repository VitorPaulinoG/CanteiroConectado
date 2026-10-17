import { Chip } from 'react-native-paper';

import { useAppTheme } from '@/theme';

export type StockStatus = 'normal' | 'belowMinimum' | 'empty';

const LABELS: Record<StockStatus, string> = {
  normal: 'Normal',
  belowMinimum: 'Abaixo do mínimo',
  empty: 'Zerado',
};

type Props = { status: StockStatus };

export function StatusBadge({ status }: Props) {
  const { status: colors } = useAppTheme();
  const containers = {
    normal: [colors.normalContainer, colors.onNormalContainer],
    belowMinimum: [colors.belowMinimumContainer, colors.onBelowMinimumContainer],
    empty: [colors.emptyContainer, colors.onEmptyContainer],
  } as const;
  const [background, text] = containers[status];

  return (
    <Chip
      compact
      style={{ backgroundColor: background, alignSelf: 'flex-start' }}
      textStyle={{ color: text }}
    >
      {LABELS[status]}
    </Chip>
  );
}
