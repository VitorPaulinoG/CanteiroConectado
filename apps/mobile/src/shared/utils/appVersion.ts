import Constants from 'expo-constants';

export function versaoDoApp(): string {
  return Constants.expoConfig?.version ?? 'desconhecida';
}
