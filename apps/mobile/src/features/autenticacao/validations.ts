export type ErrosLogin = { email?: string; senha?: string };

const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validarLogin(email: string, senha: string): ErrosLogin {
  const erros: ErrosLogin = {};
  if (!email.trim()) {
    erros.email = 'Informe o e-mail.';
  } else if (!FORMATO_EMAIL.test(email.trim())) {
    erros.email = 'Informe um e-mail válido.';
  }
  if (!senha) {
    erros.senha = 'Informe a senha.';
  }
  return erros;
}

export function temErros(erros: object): boolean {
  return Object.values(erros).some(Boolean);
}

export function minutosRestantes(bloqueadoAte: string, agora = Date.now()): number {
  const diferenca = new Date(bloqueadoAte).getTime() - agora;
  return Math.max(1, Math.ceil(diferenca / 60_000));
}
