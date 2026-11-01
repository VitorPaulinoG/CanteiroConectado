import { api, type Pagina } from '@/shared/api';

export type Colaborador = {
  id: string;
  nome: string;
  canteiroId: string;
  ativo: boolean;
};

export type FiltroColaboradores = { q?: string; ativo?: boolean };

export const chavesColaboradores = {
  todos: ['colaboradores'] as const,
  lista: (canteiroId: string, filtro: FiltroColaboradores) =>
    ['colaboradores', canteiroId, filtro] as const,
};

const BASE = '/api/v1/colaboradores';

export async function listarColaboradores(
  canteiroId: string,
  filtro: FiltroColaboradores,
  pagina: number,
): Promise<Pagina<Colaborador>> {
  const resposta = await api.get<Pagina<Colaborador>>(BASE, {
    params: { canteiroId, ...filtro, pagina },
  });
  return resposta.data;
}

export async function criarColaborador(nome: string, canteiroId: string): Promise<Colaborador> {
  return (await api.post<Colaborador>(BASE, { nome, canteiroId })).data;
}

export async function atualizarColaborador(
  id: string,
  dados: { nome?: string; ativo?: boolean },
): Promise<Colaborador> {
  return (await api.patch<Colaborador>(`${BASE}/${id}`, dados)).data;
}
