import { deData, hoje, paraData, somarDias } from '../data';
import { formatarData } from '../format';

describe('datas sem horário', () => {
  it('converte ida e volta no fuso do aparelho', () => {
    expect(deData(paraData('2026-11-05'))).toBe('2026-11-05');
    expect(hoje(new Date(2026, 0, 9, 23, 59))).toBe('2026-01-09');
  });

  it('soma dias atravessando o mês', () => {
    expect(somarDias('2026-10-30', 3)).toBe('2026-11-02');
  });

  it('formata como dd/mm/aaaa', () => {
    expect(formatarData('2026-11-05')).toBe('05/11/2026');
  });
});
