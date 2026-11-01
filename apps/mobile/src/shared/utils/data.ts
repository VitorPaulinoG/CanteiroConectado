function doisDigitos(valor: number): string {
  return String(valor).padStart(2, '0');
}

export function deData(data: Date): string {
  return `${data.getFullYear()}-${doisDigitos(data.getMonth() + 1)}-${doisDigitos(data.getDate())}`;
}

export function paraData(iso: string): Date {
  const [ano, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function hoje(agora: Date = new Date()): string {
  return deData(agora);
}

export function somarDias(iso: string, dias: number): string {
  const data = paraData(iso);
  data.setDate(data.getDate() + dias);
  return deData(data);
}
