import { formatarQuantidade, lerQuantidade } from '@/shared/utils/quantidade';

export const TAMANHO_MAXIMO_FRENTE = 60;
export const TAMANHO_MAXIMO_OBSERVACAO = 200;

export type ErrosMovimentacao = {
  itemId?: string;
  quantidade?: string;
  colaboradorId?: string;
  frenteObra?: string;
  observacao?: string;
};

export type DadosDaSaida = {
  item: { saldo?: number; unidadeSimbolo: string } | null;
  quantidade: string;
  motivo: 'RETIRADA' | 'AJUSTE';
  colaboradorId: string | null;
  frenteObra: string;
  observacao: string;
};

export function validarQuantidade(texto: string): string | undefined {
  const valor = lerQuantidade(texto);
  if (valor === null || valor <= 0) {
    return 'Informe uma quantidade maior que zero, com até 3 casas decimais.';
  }
  return undefined;
}

function validarTextos(frenteObra: string, observacao: string, erros: ErrosMovimentacao) {
  if (frenteObra.trim().length > TAMANHO_MAXIMO_FRENTE) {
    erros.frenteObra = `A frente de obra deve ter no máximo ${TAMANHO_MAXIMO_FRENTE} caracteres.`;
  }
  if (observacao.trim().length > TAMANHO_MAXIMO_OBSERVACAO) {
    erros.observacao = `A observação deve ter no máximo ${TAMANHO_MAXIMO_OBSERVACAO} caracteres.`;
  }
}

export function validarSaida(dados: DadosDaSaida): ErrosMovimentacao {
  const erros: ErrosMovimentacao = {};
  if (!dados.item) {
    erros.itemId = 'Escolha o item.';
  }
  erros.quantidade = validarQuantidade(dados.quantidade);
  const quantidade = lerQuantidade(dados.quantidade);
  if (!erros.quantidade && dados.item?.saldo !== undefined && quantidade !== null) {
    if (quantidade > dados.item.saldo) {
      erros.quantidade = `A quantidade passa do saldo (${formatarQuantidade(dados.item.saldo, dados.item.unidadeSimbolo)}).`;
    }
  }
  if (dados.motivo === 'RETIRADA') {
    if (!dados.colaboradorId) {
      erros.colaboradorId = 'Escolha quem está retirando.';
    }
    if (!dados.frenteObra.trim()) {
      erros.frenteObra = 'Informe a frente de obra.';
    }
  }
  if (dados.motivo === 'AJUSTE' && !dados.observacao.trim()) {
    erros.observacao = 'No ajuste, conte o que aconteceu (perda, furto, dano...).';
  }
  validarTextos(dados.frenteObra, dados.observacao, erros);
  return limpar(erros);
}

export type DadosDaEntrada = {
  item: { tipo: 'MATERIAL' | 'FERRAMENTA' } | null;
  quantidade: string;
  motivo: 'RECEBIMENTO' | 'DEVOLUCAO' | 'AJUSTE';
  colaboradorId: string | null;
  observacao: string;
};

export function validarEntrada(dados: DadosDaEntrada): ErrosMovimentacao {
  const erros: ErrosMovimentacao = {};
  if (!dados.item) {
    erros.itemId = 'Escolha o item.';
  }
  erros.quantidade = validarQuantidade(dados.quantidade);
  if (dados.motivo === 'DEVOLUCAO') {
    if (dados.item && dados.item.tipo !== 'FERRAMENTA') {
      erros.itemId = 'Só ferramentas podem ser devolvidas. Para material, use Ajuste.';
    }
    if (!dados.colaboradorId) {
      erros.colaboradorId = 'Escolha quem está devolvendo.';
    }
  }
  if (dados.motivo === 'AJUSTE' && !dados.observacao.trim()) {
    erros.observacao = 'No ajuste, conte o que aconteceu (contagem, sobra...).';
  }
  validarTextos('', dados.observacao, erros);
  return limpar(erros);
}

export function limpar(erros: ErrosMovimentacao): ErrosMovimentacao {
  return Object.fromEntries(
    Object.entries(erros).filter(([, mensagem]) => Boolean(mensagem)),
  ) as ErrosMovimentacao;
}

export function temErros(erros: object): boolean {
  return Object.values(erros).some(Boolean);
}

export function textoOpcional(texto: string): string | undefined {
  return texto.trim() ? texto.trim() : undefined;
}
