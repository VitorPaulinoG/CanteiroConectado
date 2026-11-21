import type { UsuarioSessao } from './tipos';

export function rotuloDoPerfil(usuario: Pick<UsuarioSessao, 'perfil' | 'administrador'>): string {
  const base = usuario.perfil === 'ALMOXARIFE' ? 'Almoxarife' : 'Gestor';
  return usuario.administrador ? `${base} · Administrador` : base;
}

export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) {
    return '?';
  }
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return `${primeira}${ultima}`.toUpperCase();
}
