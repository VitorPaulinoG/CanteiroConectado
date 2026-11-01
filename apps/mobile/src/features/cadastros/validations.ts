import { lerQuantidade } from '@/shared/utils/quantidade';

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

export type ErrosFuncionario = { nome?: string; email?: string; canteiroId?: string };

const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validarFuncionario(dados: {
  nome: string;
  email: string;
  perfil: 'ALMOXARIFE' | 'GESTOR';
  canteiroId: string | null;
}): ErrosFuncionario {
  const erros: ErrosFuncionario = {};
  if (!dados.nome.trim()) {
    erros.nome = 'Informe o nome.';
  } else if (dados.nome.trim().length > TAMANHO_MAXIMO_NOME) {
    erros.nome = `O nome deve ter no máximo ${TAMANHO_MAXIMO_NOME} caracteres.`;
  }
  if (!dados.email.trim()) {
    erros.email = 'Informe o e-mail.';
  } else if (!FORMATO_EMAIL.test(dados.email.trim())) {
    erros.email = 'Informe um e-mail válido.';
  }
  if (dados.perfil === 'ALMOXARIFE' && !dados.canteiroId) {
    erros.canteiroId = 'Escolha o canteiro do almoxarife.';
  }
  return erros;
}

export type ErrosItem = {
  codigo?: string;
  descricao?: string;
  unidade?: string;
  tipo?: string;
  quantidadeMinima?: string;
};

export const TAMANHO_MAXIMO_CODIGO = 30;
export const TAMANHO_MAXIMO_DESCRICAO = 120;

export function validarItem(dados: {
  codigo: string;
  descricao: string;
  unidade: string | null;
  tipo: string | null;
  quantidadeMinima: string;
}): ErrosItem {
  const erros: ErrosItem = {};
  if (!dados.codigo.trim()) {
    erros.codigo = 'Informe o código.';
  } else if (dados.codigo.trim().length > TAMANHO_MAXIMO_CODIGO) {
    erros.codigo = `O código deve ter no máximo ${TAMANHO_MAXIMO_CODIGO} caracteres.`;
  }
  if (!dados.descricao.trim()) {
    erros.descricao = 'Informe a descrição.';
  } else if (dados.descricao.trim().length > TAMANHO_MAXIMO_DESCRICAO) {
    erros.descricao = `A descrição deve ter no máximo ${TAMANHO_MAXIMO_DESCRICAO} caracteres.`;
  }
  if (!dados.unidade) {
    erros.unidade = 'Escolha a unidade.';
  }
  if (!dados.tipo) {
    erros.tipo = 'Escolha o tipo.';
  }
  if (dados.quantidadeMinima.trim() && lerQuantidade(dados.quantidadeMinima) === null) {
    erros.quantidadeMinima = 'Informe um número igual ou maior que zero, com até 3 casas decimais.';
  }
  return erros;
}
