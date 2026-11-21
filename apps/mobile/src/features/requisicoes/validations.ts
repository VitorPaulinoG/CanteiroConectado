import { lerQuantidade } from '@/shared/utils/quantidade';

export const MAXIMO_DE_ITENS = 20;
export const TAMANHO_MAXIMO_FRENTE = 60;
export const TAMANHO_MAXIMO_OBSERVACAO = 200;

export type LinhaDaRequisicao = {
  itemId: string;
  codigo: string;
  descricao: string;
  unidadeSimbolo: string;
  saldo?: number;
  quantidade: string;
};

export type ErrosRequisicao = Record<string, string>;

type Dados = {
  linhas: LinhaDaRequisicao[];
  necessarioAte: string | null;
  frenteObra: string;
  observacao: string;
  hoje: string;
};

export function validarRequisicao({ linhas, necessarioAte, frenteObra, observacao, hoje }: Dados) {
  const erros: ErrosRequisicao = {};
  if (linhas.length === 0) {
    erros.itens = 'Adicione pelo menos um item.';
  } else if (linhas.length > MAXIMO_DE_ITENS) {
    erros.itens = `Uma requisição aceita no máximo ${MAXIMO_DE_ITENS} itens.`;
  }
  linhas.forEach((linha, indice) => {
    const quantidade = lerQuantidade(linha.quantidade);
    if (quantidade === null || quantidade <= 0) {
      erros[`itens[${indice}].quantidade`] = 'Informe uma quantidade maior que zero.';
    }
  });
  if (!necessarioAte) {
    erros.necessarioAte = 'Informe até quando o material é necessário.';
  } else if (necessarioAte < hoje) {
    erros.necessarioAte = 'A data de necessidade não pode ser anterior a hoje.';
  }
  if (frenteObra.trim().length > TAMANHO_MAXIMO_FRENTE) {
    erros.frenteObra = `A frente de obra deve ter no máximo ${TAMANHO_MAXIMO_FRENTE} caracteres.`;
  }
  if (observacao.trim().length > TAMANHO_MAXIMO_OBSERVACAO) {
    erros.observacao = `A observação deve ter no máximo ${TAMANHO_MAXIMO_OBSERVACAO} caracteres.`;
  }
  return erros;
}

export function temErros(erros: object): boolean {
  return Object.values(erros).some(Boolean);
}

export const TAMANHO_MINIMO_DO_MOTIVO = 5;

export type ErrosDecisao = { previsaoChegada?: string; motivo?: string; observacao?: string };

export function validarDecisao(dados: {
  decisao: 'aprovar' | 'recusar';
  previsaoChegada: string | null;
  motivo: string;
  observacao: string;
  hoje: string;
}): ErrosDecisao {
  const erros: ErrosDecisao = {};
  if (dados.decisao === 'aprovar') {
    if (dados.previsaoChegada && dados.previsaoChegada < dados.hoje) {
      erros.previsaoChegada = 'A previsão de chegada não pode ser anterior a hoje.';
    }
    if (dados.observacao.trim().length > TAMANHO_MAXIMO_OBSERVACAO) {
      erros.observacao = `A observação deve ter no máximo ${TAMANHO_MAXIMO_OBSERVACAO} caracteres.`;
    }
  } else if (dados.motivo.trim().length < TAMANHO_MINIMO_DO_MOTIVO) {
    erros.motivo = `Conte o motivo da recusa (pelo menos ${TAMANHO_MINIMO_DO_MOTIVO} caracteres).`;
  } else if (dados.motivo.trim().length > TAMANHO_MAXIMO_OBSERVACAO) {
    erros.motivo = `O motivo deve ter no máximo ${TAMANHO_MAXIMO_OBSERVACAO} caracteres.`;
  }
  return erros;
}
