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

export const TAMANHO_MINIMO_SENHA = 8;

export type RegraDeSenha = { descricao: string; atendida: boolean };

export function regrasDaNovaSenha(
  senhaAtual: string,
  novaSenha: string,
  confirmacao: string,
): RegraDeSenha[] {
  return [
    {
      descricao: `Pelo menos ${TAMANHO_MINIMO_SENHA} caracteres`,
      atendida: novaSenha.length >= TAMANHO_MINIMO_SENHA,
    },
    { descricao: 'Pelo menos uma letra', atendida: /\p{L}/u.test(novaSenha) },
    { descricao: 'Pelo menos um número', atendida: /\d/.test(novaSenha) },
    {
      descricao: 'Diferente da senha atual',
      atendida: novaSenha.length > 0 && novaSenha !== senhaAtual,
    },
    {
      descricao: 'Confirmação igual à nova senha',
      atendida: confirmacao.length > 0 && confirmacao === novaSenha,
    },
  ];
}

export type ErrosTrocaDeSenha = { senhaAtual?: string; novaSenha?: string; confirmacao?: string };

export function validarTrocaDeSenha(
  senhaAtual: string,
  novaSenha: string,
  confirmacao: string,
): ErrosTrocaDeSenha {
  const erros: ErrosTrocaDeSenha = {};
  if (!senhaAtual) {
    erros.senhaAtual = 'Informe a senha atual.';
  }
  const [tamanho, letra, numero, diferente] = regrasDaNovaSenha(senhaAtual, novaSenha, confirmacao);
  if (!tamanho.atendida || !letra.atendida || !numero.atendida) {
    erros.novaSenha = 'A nova senha não atende às regras abaixo.';
  } else if (!diferente.atendida) {
    erros.novaSenha = 'A nova senha deve ser diferente da atual.';
  }
  if (confirmacao !== novaSenha) {
    erros.confirmacao = 'A confirmação não confere com a nova senha.';
  }
  return erros;
}
