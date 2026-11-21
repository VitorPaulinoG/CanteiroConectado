import * as SecureStore from 'expo-secure-store';

export function chaveDoCanteiroAtivo(usuarioId: string): string {
  return `canteiro.ativo.${usuarioId.replace(/[^A-Za-z0-9._-]/g, '')}`;
}

export async function lerCanteiroAtivo(usuarioId: string): Promise<string | null> {
  return SecureStore.getItemAsync(chaveDoCanteiroAtivo(usuarioId));
}

export async function salvarCanteiroAtivo(usuarioId: string, canteiroId: string): Promise<void> {
  await SecureStore.setItemAsync(chaveDoCanteiroAtivo(usuarioId), canteiroId);
}
