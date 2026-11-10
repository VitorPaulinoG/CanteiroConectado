import { api } from '@/shared/api';
import type { UsuarioSessao } from '@/shared/sessao';

export type LoginResposta = {
  token: string;
  expiraEm: string;
  usuario: UsuarioSessao;
};

export async function postarLogin(email: string, senha: string): Promise<LoginResposta> {
  const resposta = await api.post<LoginResposta>('/api/v1/autenticacao/login', {
    email: email.trim(),
    senha,
  });
  return resposta.data;
}

export async function buscarEu(): Promise<UsuarioSessao> {
  const resposta = await api.get<UsuarioSessao>('/api/v1/autenticacao/eu');
  return resposta.data;
}
