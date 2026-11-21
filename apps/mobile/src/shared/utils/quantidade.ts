export const CASAS_DECIMAIS = 3;

export function lerQuantidade(texto: string): number | null {
  const limpo = texto.trim().replace(/\s/g, '');
  if (!/^\d+([.,]\d{1,3})?$/.test(limpo)) {
    return null;
  }
  return Number(limpo.replace(',', '.'));
}

export function formatarNumero(valor: number): string {
  return valor.toLocaleString('pt-BR', { maximumFractionDigits: CASAS_DECIMAIS });
}

export function formatarQuantidade(valor: number, unidadeSimbolo: string): string {
  return `${formatarNumero(valor)} ${unidadeSimbolo}`;
}
