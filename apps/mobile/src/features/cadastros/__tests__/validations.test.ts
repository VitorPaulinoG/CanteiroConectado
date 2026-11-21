import { temErros, validarCanteiro, validarFuncionario, validarItem } from '../validations';

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

describe('validarItem', () => {
  const valido = {
    codigo: 'FER-0012',
    descricao: 'Furadeira',
    unidade: 'UN',
    tipo: 'FERRAMENTA',
    quantidadeMinima: '',
  };

  it('aceita mínimo vazio, zero ou decimal com vírgula', () => {
    expect(validarItem(valido)).toEqual({});
    expect(validarItem({ ...valido, quantidadeMinima: '0' })).toEqual({});
    expect(validarItem({ ...valido, quantidadeMinima: '2,5' })).toEqual({});
  });

  it('limita o código a 30 caracteres', () => {
    expect(validarItem({ ...valido, codigo: 'X'.repeat(31) }).codigo).toBe(
      'O código deve ter no máximo 30 caracteres.',
    );
  });
});

describe('limites e campos vazios do funcionário e do item', () => {
  it('exige o e-mail e limita o nome do funcionário a 80 caracteres', () => {
    const erros = validarFuncionario({
      nome: 'f'.repeat(81),
      email: '  ',
      perfil: 'GESTOR',
      canteiroId: null,
    });
    expect(erros.nome).toBe('O nome deve ter no máximo 80 caracteres.');
    expect(erros.email).toBe('Informe o e-mail.');
  });

  it('exige a descrição do item e a limita a 120 caracteres', () => {
    const item = { codigo: 'MAT-1', unidade: 'SACO', tipo: 'MATERIAL', quantidadeMinima: '' };
    expect(validarItem({ ...item, descricao: ' ' }).descricao).toBe('Informe a descrição.');
    expect(validarItem({ ...item, descricao: 'g'.repeat(121) }).descricao).toBe(
      'A descrição deve ter no máximo 120 caracteres.',
    );
  });
});
