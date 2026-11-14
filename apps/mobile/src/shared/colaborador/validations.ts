export const TAMANHO_MAXIMO_NOME_COLABORADOR = 80;

export function validarNomeColaborador(nome: string): string | undefined {
  if (!nome.trim()) {
    return 'Informe o nome do colaborador.';
  }
  if (nome.trim().length > TAMANHO_MAXIMO_NOME_COLABORADOR) {
    return `O nome deve ter no máximo ${TAMANHO_MAXIMO_NOME_COLABORADOR} caracteres.`;
  }
  return undefined;
}
