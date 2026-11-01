export function gerarRequestId(): string {
  const tempo = Date.now().toString(36);
  const aleatorio = Math.random().toString(36).slice(2, 12);
  return `app-${tempo}-${aleatorio}`;
}
