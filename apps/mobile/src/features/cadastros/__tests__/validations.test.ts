import { temErros, validarCanteiro, validarFuncionario } from '../validations';

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

describe('validarFuncionario', () => {
  it('exige nome, e-mail válido e canteiro do almoxarife', () => {
    expect(
      validarFuncionario({ nome: '', email: 'x', perfil: 'ALMOXARIFE', canteiroId: null }),
    ).toEqual({
      nome: 'Informe o nome.',
      email: 'Informe um e-mail válido.',
      canteiroId: 'Escolha o canteiro do almoxarife.',
    });
  });

  it('não exige canteiro do gestor', () => {
    expect(
      validarFuncionario({
        nome: 'Camila',
        email: 'c@obra.com',
        perfil: 'GESTOR',
        canteiroId: null,
      }),
    ).toEqual({});
  });
});
