import { isAxiosError } from 'axios';

import { mensagemPara } from './messages';

export type ProblemDetails = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  codigo?: string;
  requestId?: string;
  campos?: string[];
  [propriedade: string]: unknown;
};

type DadosAppError = {
  codigo: string;
  mensagem: string;
  status?: number;
  requestId?: string;
  campos?: string[];
  detalhe?: string;
  extras?: Record<string, unknown>;
  semResposta?: boolean;
};

export class AppError extends Error {
  readonly codigo: string;
  readonly status?: number;
  readonly requestId?: string;
  readonly campos: string[];
  readonly detalhe?: string;
  readonly extras: Record<string, unknown>;
  readonly semResposta: boolean;

  constructor(dados: DadosAppError) {
    super(dados.mensagem);
    this.name = 'AppError';
    this.codigo = dados.codigo;
    this.status = dados.status;
    this.requestId = dados.requestId;
    this.campos = dados.campos ?? [];
    this.detalhe = dados.detalhe;
    this.extras = dados.extras ?? {};
    this.semResposta = dados.semResposta ?? false;
  }

  get erroDoCliente(): boolean {
    return this.status !== undefined && this.status >= 400 && this.status < 500;
  }
}

function ehProblemDetails(dados: unknown): dados is ProblemDetails {
  return typeof dados === 'object' && dados !== null && 'codigo' in dados;
}

const CODIGOS_DE_TEMPO = new Set(['ECONNABORTED', 'ETIMEDOUT']);

export function paraAppError(erro: unknown): AppError {
  if (erro instanceof AppError) {
    return erro;
  }
  if (isAxiosError(erro)) {
    if (!erro.response) {
      const codigo = CODIGOS_DE_TEMPO.has(erro.code ?? '') ? 'TEMPO_ESGOTADO' : 'SEM_CONEXAO';
      return new AppError({ codigo, mensagem: mensagemPara(codigo), semResposta: true });
    }
    const { status, data } = erro.response;
    if (ehProblemDetails(data)) {
      const { codigo = 'ERRO_INESPERADO', detail, requestId, campos, ...resto } = data;
      return new AppError({
        codigo,
        mensagem: mensagemPara(codigo, detail),
        status,
        requestId,
        campos,
        detalhe: detail,
        extras: resto,
      });
    }
    return new AppError({
      codigo: 'ERRO_INESPERADO',
      mensagem: mensagemPara('ERRO_INESPERADO'),
      status,
    });
  }
  return new AppError({ codigo: 'ERRO_INESPERADO', mensagem: mensagemPara('ERRO_INESPERADO') });
}
