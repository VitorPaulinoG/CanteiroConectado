import { validarRequisicao, type LinhaDaRequisicao } from '../validations';

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
