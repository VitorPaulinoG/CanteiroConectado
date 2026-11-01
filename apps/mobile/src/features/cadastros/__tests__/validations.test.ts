import { temErros, validarCanteiro } from '../validations';

describe('validarCanteiro', () => {
  it('exige o nome', () => {
    expect(validarCanteiro('  ', '')).toEqual({ nome: 'Informe o nome do canteiro.' });
  });

  it('limita o tamanho dos campos', () => {
    const erros = validarCanteiro('a'.repeat(81), 'b'.repeat(201));
    expect(erros.nome).toBe('O nome deve ter no máximo 80 caracteres.');
    expect(erros.endereco).toBe('O endereço deve ter no máximo 200 caracteres.');
  });

  it('aceita nome com endereço vazio', () => {
    expect(temErros(validarCanteiro('Residencial Aurora', ''))).toBe(false);
  });
});
