import { validarEntrada, validarSaida, type DadosDaSaida } from '../validations';

const base: DadosDaSaida = {
  item: { saldo: 2, unidadeSimbolo: 'un' },
  quantidade: '1',
  motivo: 'RETIRADA',
  colaboradorId: 'col-1',
  frenteObra: 'Fundação',
  observacao: '',
};

describe('validarSaida', () => {
  it('aceita a retirada completa', () => {
    expect(validarSaida(base)).toEqual({});
  });

  it('impede quantidade acima do saldo no próprio campo', () => {
    expect(validarSaida({ ...base, quantidade: '3' }).quantidade).toBe(
      'A quantidade passa do saldo (2 un).',
    );
  });

  it.each(['0', '-1', '', 'abc', '1,2345'])('recusa a quantidade %s', (quantidade) => {
    expect(validarSaida({ ...base, quantidade }).quantidade).toBeDefined();
  });

  it('exige item, colaborador e frente na retirada', () => {
    expect(validarSaida({ ...base, item: null, colaboradorId: null, frenteObra: '  ' })).toEqual({
      itemId: 'Escolha o item.',
      colaboradorId: 'Escolha quem está retirando.',
      frenteObra: 'Informe a frente de obra.',
    });
  });

  it('no ajuste dispensa colaborador e frente e exige a observação', () => {
    expect(
      validarSaida({ ...base, motivo: 'AJUSTE', colaboradorId: null, frenteObra: '' }),
    ).toEqual({ observacao: 'No ajuste, conte o que aconteceu (perda, furto, dano...).' });
  });

  it('limita frente a 60 e observação a 200 caracteres', () => {
    const erros = validarSaida({
      ...base,
      frenteObra: 'x'.repeat(61),
      observacao: 'y'.repeat(201),
    });
    expect(erros.frenteObra).toBe('A frente de obra deve ter no máximo 60 caracteres.');
    expect(erros.observacao).toBe('A observação deve ter no máximo 200 caracteres.');
  });
});

describe('validarEntrada', () => {
  const ferramenta = { tipo: 'FERRAMENTA' as const };
  const material = { tipo: 'MATERIAL' as const };

  it('aceita o recebimento sem colaborador nem observação', () => {
    expect(
      validarEntrada({
        item: material,
        quantidade: '50',
        motivo: 'RECEBIMENTO',
        colaboradorId: null,
        observacao: '',
      }),
    ).toEqual({});
  });

  it('na devolução exige colaborador e ferramenta', () => {
    expect(
      validarEntrada({
        item: material,
        quantidade: '1',
        motivo: 'DEVOLUCAO',
        colaboradorId: null,
        observacao: '',
      }),
    ).toEqual({
      itemId: 'Só ferramentas podem ser devolvidas. Para material, use Ajuste.',
      colaboradorId: 'Escolha quem está devolvendo.',
    });
    expect(
      validarEntrada({
        item: ferramenta,
        quantidade: '1',
        motivo: 'DEVOLUCAO',
        colaboradorId: 'c',
        observacao: '',
      }),
    ).toEqual({});
  });

  it('no ajuste exige a observação', () => {
    expect(
      validarEntrada({
        item: material,
        quantidade: '2',
        motivo: 'AJUSTE',
        colaboradorId: null,
        observacao: ' ',
      }).observacao,
    ).toBe('No ajuste, conte o que aconteceu (contagem, sobra...).');
  });

  it('recusa quantidade zero ou negativa', () => {
    expect(
      validarEntrada({
        item: material,
        quantidade: '0',
        motivo: 'RECEBIMENTO',
        colaboradorId: null,
        observacao: '',
      }).quantidade,
    ).toBeDefined();
  });
});
