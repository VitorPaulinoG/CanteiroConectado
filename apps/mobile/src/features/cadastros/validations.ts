export type ErrosCanteiro = { nome?: string; endereco?: string };

export const TAMANHO_MAXIMO_NOME = 80;
export const TAMANHO_MAXIMO_ENDERECO = 200;

export function validarCanteiro(nome: string, endereco: string): ErrosCanteiro {
  const erros: ErrosCanteiro = {};
  if (!nome.trim()) {
    erros.nome = 'Informe o nome do canteiro.';
  } else if (nome.trim().length > TAMANHO_MAXIMO_NOME) {
    erros.nome = `O nome deve ter no máximo ${TAMANHO_MAXIMO_NOME} caracteres.`;
  }
  if (endereco.trim().length > TAMANHO_MAXIMO_ENDERECO) {
    erros.endereco = `O endereço deve ter no máximo ${TAMANHO_MAXIMO_ENDERECO} caracteres.`;
  }
  return erros;
}

export function temErros(erros: object): boolean {
  return Object.values(erros).some(Boolean);
}
