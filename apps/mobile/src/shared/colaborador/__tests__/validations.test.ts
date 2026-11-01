import { validarNomeColaborador } from '../validations';

describe('validarNomeColaborador', () => {
  it('exige o nome', () => {
    expect(validarNomeColaborador('  ')).toBe('Informe o nome do colaborador.');
  });

  it('limita o nome a 80 caracteres sem contar os espaços das pontas', () => {
    expect(validarNomeColaborador('j'.repeat(81))).toBe('O nome deve ter no máximo 80 caracteres.');
    expect(validarNomeColaborador(` ${'j'.repeat(80)} `)).toBeUndefined();
  });
});
