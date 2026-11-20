import { validarDecisao, validarRequisicao, type LinhaDaRequisicao } from '../validations';

const linha = (quantidade: string, itemId = 'i1'): LinhaDaRequisicao => ({
  itemId,
  codigo: 'MAT-1',
  descricao: 'Cimento',
  unidadeSimbolo: 'saco',
  quantidade,
});

const base = {
  linhas: [linha('40')],
  necessarioAte: '2026-11-10',
  frenteObra: '',
  observacao: '',
  hoje: '2026-11-01',
};

describe('validarRequisicao', () => {
  it('aceita a requisição completa e a data de hoje', () => {
    expect(validarRequisicao(base)).toEqual({});
    expect(validarRequisicao({ ...base, necessarioAte: '2026-11-01' })).toEqual({});
  });

  it('exige ao menos um item e no máximo 20', () => {
    expect(validarRequisicao({ ...base, linhas: [] }).itens).toBe('Adicione pelo menos um item.');
    const vinteEUm = Array.from({ length: 21 }, (_, i) => linha('1', `i${i}`));
    expect(validarRequisicao({ ...base, linhas: vinteEUm }).itens).toBe(
      'Uma requisição aceita no máximo 20 itens.',
    );
  });

  it('aponta a linha com quantidade inválida', () => {
    const erros = validarRequisicao({ ...base, linhas: [linha('40'), linha('0', 'i2')] });
    expect(erros).toEqual({ 'itens[1].quantidade': 'Informe uma quantidade maior que zero.' });
  });

  it('exige a data e recusa data anterior a hoje', () => {
    expect(validarRequisicao({ ...base, necessarioAte: null }).necessarioAte).toBe(
      'Informe até quando o material é necessário.',
    );
    expect(validarRequisicao({ ...base, necessarioAte: '2026-10-31' }).necessarioAte).toBe(
      'A data de necessidade não pode ser anterior a hoje.',
    );
  });
});

describe('validarDecisao', () => {
  const base = { previsaoChegada: null, motivo: '', observacao: '', hoje: '2026-11-01' };

  it('aprova sem previsão e recusa previsão no passado', () => {
    expect(validarDecisao({ ...base, decisao: 'aprovar' })).toEqual({});
    expect(
      validarDecisao({ ...base, decisao: 'aprovar', previsaoChegada: '2026-10-31' })
        .previsaoChegada,
    ).toBe('A previsão de chegada não pode ser anterior a hoje.');
  });

  it('exige motivo de 5 caracteres ou mais na recusa', () => {
    expect(validarDecisao({ ...base, decisao: 'recusar', motivo: ' não ' }).motivo).toBe(
      'Conte o motivo da recusa (pelo menos 5 caracteres).',
    );
    expect(validarDecisao({ ...base, decisao: 'recusar', motivo: 'Sem verba' })).toEqual({});
  });
});

describe('limites de texto da requisição e da decisão', () => {
  const requisicao = {
    linhas: [linha('40')],
    necessarioAte: '2026-11-10',
    frenteObra: '',
    observacao: '',
    hoje: '2026-11-01',
  };
  const decisao = { previsaoChegada: null, motivo: '', observacao: '', hoje: '2026-11-01' };

  it('limita a frente a 60 e a observação a 200 caracteres, sem contar os espaços das pontas', () => {
    const erros = validarRequisicao({
      ...requisicao,
      frenteObra: 'a'.repeat(61),
      observacao: 'b'.repeat(201),
    });
    expect(erros.frenteObra).toBe('A frente de obra deve ter no máximo 60 caracteres.');
    expect(erros.observacao).toBe('A observação deve ter no máximo 200 caracteres.');
    expect(
      validarRequisicao({ ...requisicao, frenteObra: ` ${'a'.repeat(60)} `, observacao: '' }),
    ).toEqual({});
  });

  it('limita a observação da aprovação e o motivo da recusa a 200 caracteres', () => {
    expect(
      validarDecisao({ ...decisao, decisao: 'aprovar', observacao: 'c'.repeat(201) }).observacao,
    ).toBe('A observação deve ter no máximo 200 caracteres.');
    expect(validarDecisao({ ...decisao, decisao: 'recusar', motivo: 'd'.repeat(201) }).motivo).toBe(
      'O motivo deve ter no máximo 200 caracteres.',
    );
  });
});
