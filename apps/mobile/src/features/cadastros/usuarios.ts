import { api, type Pagina } from '@/shared/api';
import type { Perfil } from '@/shared/sessao';

export type Funcionario = {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  administrador: boolean;
  canteiroId: string | null;
  ativo: boolean;
  precisaTrocarSenha: boolean;
  credencialInicialDisponivel: boolean;
  criadoEm: string;
};

export type CredencialInicial = { email: string; senhaTemporaria: string };

export type FiltroFuncionarios = { q?: string; perfil?: Perfil; ativo?: boolean };

export type DadosFuncionario = {
  nome: string;
  email: string;
  perfil: Perfil;
  canteiroId: string | null;
};

export const chavesFuncionarios = {
  todos: ['usuarios'] as const,
  lista: (filtro: FiltroFuncionarios) => ['usuarios', 'lista', filtro] as const,
  detalhe: (id: string) => ['usuarios', 'detalhe', id] as const,
  credencial: (id: string) => ['usuarios', 'credencial', id] as const,
};

const BASE = '/api/v1/usuarios';

export async function listarFuncionarios(
  filtro: FiltroFuncionarios,
  pagina: number,
): Promise<Pagina<Funcionario>> {
  const resposta = await api.get<Pagina<Funcionario>>(BASE, { params: { ...filtro, pagina } });
  return resposta.data;
}

export async function buscarFuncionario(id: string): Promise<Funcionario> {
  return (await api.get<Funcionario>(`${BASE}/${id}`)).data;
}

export async function criarFuncionario(dados: DadosFuncionario): Promise<Funcionario> {
  return (await api.post<Funcionario>(BASE, dados)).data;
}

export async function atualizarFuncionario(
  id: string,
  dados: DadosFuncionario,
): Promise<Funcionario> {
  return (await api.patch<Funcionario>(`${BASE}/${id}`, dados)).data;
}

export async function redefinirSenha(id: string): Promise<Funcionario> {
  return (await api.post<Funcionario>(`${BASE}/${id}/redefinir-senha`)).data;
}

export async function desativarFuncionario(id: string): Promise<Funcionario> {
  return (await api.post<Funcionario>(`${BASE}/${id}/desativar`)).data;
}

export async function ativarFuncionario(id: string): Promise<Funcionario> {
  return (await api.post<Funcionario>(`${BASE}/${id}/ativar`)).data;
}

export async function buscarCredencialInicial(id: string): Promise<CredencialInicial> {
  return (await api.get<CredencialInicial>(`${BASE}/${id}/credencial-inicial`)).data;
}
