export type Perfil = 'ALMOXARIFE' | 'GESTOR';

export type UsuarioSessao = {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  administrador: boolean;
  canteiroId: string | null;
  precisaTrocarSenha: boolean;
};

export type EstadoSessao =
  | { status: 'carregando' }
  | { status: 'anonimo'; aviso?: string }
  | { status: 'autenticado'; usuario: UsuarioSessao; trocaAdiada: boolean };

export type ValorSessao = {
  estado: EstadoSessao;
  entrar: (email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
  atualizarUsuario: (usuario: UsuarioSessao) => void;
  adiarTroca: () => void;
};

export const AVISO_SESSAO_EXPIRADA = 'Entre de novo para continuar.';

export function trocaDeSenhaPendente(estado: EstadoSessao): boolean {
  return (
    estado.status === 'autenticado' && estado.usuario.precisaTrocarSenha && !estado.trocaAdiada
  );
}
