import {
  minutosRestantes,
  regrasDaNovaSenha,
  temErros,
  validarLogin,
  validarTrocaDeSenha,
} from '../validations';

describe('validarLogin', () => {
  it('exige e-mail e senha', () => {
    expect(validarLogin('', '')).toEqual({ email: 'Informe o e-mail.', senha: 'Informe a senha.' });
  });

  it('recusa e-mail sem formato válido', () => {
    expect(validarLogin('sebastiao', 'x').email).toBe('Informe um e-mail válido.');
  });

  it('aceita e-mail com espaços nas pontas', () => {
    expect(temErros(validarLogin('  sebastiao@obra.com ', 'senha'))).toBe(false);
  });
});

describe('minutosRestantes', () => {
  const agora = Date.parse('2026-11-01T12:00:00Z');

  it('arredonda para cima', () => {
    expect(minutosRestantes('2026-11-01T12:04:10Z', agora)).toBe(5);
  });

  it('nunca devolve menos de 1', () => {
    expect(minutosRestantes('2026-11-01T11:59:00Z', agora)).toBe(1);
  });
});

describe('regrasDaNovaSenha', () => {
  it('marca cada regra atendida', () => {
    const regras = regrasDaNovaSenha('temporaria1', 'canteiro9', 'canteiro9');
    expect(regras.every((regra) => regra.atendida)).toBe(true);
  });

  it('aponta as regras pendentes', () => {
    const pendentes = regrasDaNovaSenha('abc', 'abc', '')
      .filter((regra) => !regra.atendida)
      .map((regra) => regra.descricao);
    expect(pendentes).toEqual([
      'Pelo menos 8 caracteres',
      'Pelo menos um número',
      'Diferente da senha atual',
      'Confirmação igual à nova senha',
    ]);
  });
});

describe('validarTrocaDeSenha', () => {
  it('exige a senha atual e a confirmação igual', () => {
    expect(validarTrocaDeSenha('', 'canteiro9', 'canteiro8')).toEqual({
      senhaAtual: 'Informe a senha atual.',
      confirmacao: 'A confirmação não confere com a nova senha.',
    });
  });

  it('recusa nova senha fraca ou igual à atual', () => {
    expect(validarTrocaDeSenha('a', 'curta', 'curta').novaSenha).toBe(
      'A nova senha não atende às regras abaixo.',
    );
    expect(validarTrocaDeSenha('canteiro9', 'canteiro9', 'canteiro9').novaSenha).toBe(
      'A nova senha deve ser diferente da atual.',
    );
  });

  it('aceita senha válida', () => {
    expect(validarTrocaDeSenha('temporaria1', 'canteiro9', 'canteiro9')).toEqual({});
  });
});
