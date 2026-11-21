import {
  FILTROS_VAZIOS,
  intervaloDoPeriodo,
  paraParametros,
  quantosFiltrosExtras,
  rotuloDoDia,
  temFiltros,
} from '../filtros';

const agora = new Date(2026, 10, 5, 14, 30);

describe('intervaloDoPeriodo', () => {
  it('usa o dia local de hoje, do começo até o começo de amanhã', () => {
    expect(intervaloDoPeriodo({ ...FILTROS_VAZIOS, periodo: 'hoje' }, agora)).toEqual({
      de: new Date(2026, 10, 5),
      ate: new Date(2026, 10, 6),
    });
  });

  it('cobre os últimos 7 dias contando hoje', () => {
    expect(intervaloDoPeriodo({ ...FILTROS_VAZIOS, periodo: '7dias' }, agora)).toEqual({
      de: new Date(2026, 9, 30),
      ate: new Date(2026, 10, 6),
    });
  });

  it('inclui o último dia do período personalizado', () => {
    expect(
      intervaloDoPeriodo({ periodo: 'personalizado', de: '2026-10-01', ate: '2026-10-31' }, agora),
    ).toEqual({ de: new Date(2026, 9, 1), ate: new Date(2026, 10, 1) });
  });
});

describe('paraParametros', () => {
  it('manda as datas em UTC e os filtros escolhidos', () => {
    const parametros = paraParametros(
      {
        periodo: 'hoje',
        de: null,
        ate: null,
        tipo: 'SAIDA',
        item: { itemId: 'i1', descricao: 'Furadeira' },
        colaborador: { id: 'c1', nome: 'João' },
      },
      agora,
    );
    expect(parametros).toEqual({
      de: new Date(2026, 10, 5).toISOString(),
      ate: new Date(2026, 10, 6).toISOString(),
      tipo: 'SAIDA',
      itemId: 'i1',
      colaboradorId: 'c1',
    });
    expect(parametros.de).toMatch(/Z$/);
  });

  it('não manda nada sem filtros', () => {
    expect(paraParametros(FILTROS_VAZIOS, agora)).toEqual({
      de: undefined,
      ate: undefined,
      tipo: undefined,
      itemId: undefined,
      colaboradorId: undefined,
    });
  });
});

describe('contagem e rótulos', () => {
  it('conta os filtros extras e diz se há filtro', () => {
    expect(temFiltros(FILTROS_VAZIOS)).toBe(false);
    expect(quantosFiltrosExtras({ ...FILTROS_VAZIOS, tipo: 'ENTRADA' })).toBe(1);
    expect(temFiltros({ ...FILTROS_VAZIOS, periodo: 'hoje' })).toBe(true);
  });

  it('chama os dias de Hoje, Ontem e pela data', () => {
    expect(rotuloDoDia(new Date(2026, 10, 5, 8).toISOString(), agora)).toBe('Hoje');
    expect(rotuloDoDia(new Date(2026, 10, 4, 23).toISOString(), agora)).toBe('Ontem');
    expect(rotuloDoDia(new Date(2026, 9, 28, 9).toISOString(), agora)).toBe('28/10/2026');
  });
});
