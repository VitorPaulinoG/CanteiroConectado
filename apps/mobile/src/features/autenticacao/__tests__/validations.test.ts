import { minutosRestantes, temErros, validarLogin } from '../validations';

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
