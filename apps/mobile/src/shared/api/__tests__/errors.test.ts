import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

import { AppError, paraAppError } from '../errors';

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;

function erroComResposta(status: number, data: unknown) {
  return new AxiosError(
    'falhou',
    'ERR_BAD_REQUEST',
    config,
    {},
    {
      status,
      statusText: '',
      headers: {},
      config,
      data,
    },
  );
}

describe('paraAppError', () => {
  it('converte Problem Details com código conhecido em mensagem pt-BR', () => {
    const erro = paraAppError(
      erroComResposta(403, {
        status: 403,
        detail: 'Você não tem permissão para esta ação.',
        codigo: 'ACESSO_NEGADO',
        requestId: 'req-1',
      }),
    );

    expect(erro).toBeInstanceOf(AppError);
    expect(erro.codigo).toBe('ACESSO_NEGADO');
    expect(erro.message).toBe('Você não tem permissão para esta ação.');
    expect(erro.requestId).toBe('req-1');
    expect(erro.status).toBe(403);
    expect(erro.erroDoCliente).toBe(true);
    expect(erro.semResposta).toBe(false);
  });

  it('usa o detail da API quando o código ainda não tem tradução', () => {
    const erro = paraAppError(
      erroComResposta(422, { codigo: 'CODIGO_NOVO', detail: 'Mensagem vinda da API.' }),
    );
    expect(erro.message).toBe('Mensagem vinda da API.');
  });

  it('guarda os campos e as propriedades extras', () => {
    const erro = paraAppError(
      erroComResposta(400, {
        codigo: 'VALIDACAO_INVALIDA',
        campos: ['email: Informe um e-mail válido'],
        bloqueadoAte: '2026-11-01T12:00:00Z',
      }),
    );
    expect(erro.campos).toEqual(['email: Informe um e-mail válido']);
    expect(erro.extras.bloqueadoAte).toBe('2026-11-01T12:00:00Z');
  });

  it('trata resposta sem Problem Details como erro inesperado', () => {
    const erro = paraAppError(erroComResposta(502, '<html>Bad Gateway</html>'));
    expect(erro.codigo).toBe('ERRO_INESPERADO');
    expect(erro.status).toBe(502);
    expect(erro.erroDoCliente).toBe(false);
  });

  it('identifica falha de rede sem resposta', () => {
    const erro = paraAppError(new AxiosError('Network Error', 'ERR_NETWORK', config));
    expect(erro.codigo).toBe('SEM_CONEXAO');
    expect(erro.semResposta).toBe(true);
  });

  it('identifica tempo esgotado', () => {
    const erro = paraAppError(new AxiosError('timeout', 'ECONNABORTED', config));
    expect(erro.codigo).toBe('TEMPO_ESGOTADO');
    expect(erro.semResposta).toBe(true);
  });

  it('converte erro qualquer em erro inesperado', () => {
    expect(paraAppError(new Error('x')).codigo).toBe('ERRO_INESPERADO');
  });

  it('devolve o mesmo AppError', () => {
    const original = new AppError({ codigo: 'X', mensagem: 'y' });
    expect(paraAppError(original)).toBe(original);
  });
});
