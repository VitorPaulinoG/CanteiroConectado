import { MD3LightTheme, useTheme, type MD3Theme } from 'react-native-paper';

export type StatusColors = {
  normal: string;
  normalContainer: string;
  onNormalContainer: string;
  belowMinimum: string;
  belowMinimumContainer: string;
  onBelowMinimumContainer: string;
  empty: string;
  emptyContainer: string;
  onEmptyContainer: string;
};

export type AppTheme = MD3Theme & { status: StatusColors };

export const appTheme: AppTheme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    // Laranja de segurança
    primary: '#E65100',
    onPrimary: '#FFFFFF',
    primaryContainer: '#FFDBCB',
    onPrimaryContainer: '#380D00',
  },
  status: {
    normal: '#2E7D32',
    normalContainer: '#C8E6C9',
    onNormalContainer: '#0B2E0E',
    belowMinimum: '#F9A825',
    belowMinimumContainer: '#FFECB3',
    onBelowMinimumContainer: '#3D2A00',
    empty: '#C62828',
    emptyContainer: '#FFCDD2',
    onEmptyContainer: '#410002',
  },
};

export const useAppTheme = () => useTheme<AppTheme>();
