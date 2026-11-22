import { formatarQuantidade, lerQuantidade } from '../quantidade';

describe('lerQuantidade', () => {
  it.each([
    ['1', 1],
    ['40', 40],
    ['2,5', 2.5],
    ['0.125', 0.125],
    [' 3 ', 3],
  ])('lê %s', (texto, esperado) => {
    expect(lerQuantidade(texto)).toBe(esperado);
  });

  it.each(['', '-1', 'abc', '1,2345', '1.2.3', '1e3'])('recusa %s', (texto) => {
    expect(lerQuantidade(texto)).toBeNull();
  });
});

describe('formatarQuantidade', () => {
  it('usa vírgula e até 3 casas', () => {
    expect(formatarQuantidade(2.5, 'm³')).toBe('2,5 m³');
    expect(formatarQuantidade(1000, 'saco')).toBe('1.000 saco');
  });
});
