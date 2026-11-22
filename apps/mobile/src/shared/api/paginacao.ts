import type { Pagina } from './tipos';

export function proximaPagina<T>(ultima: Pagina<T>): number | undefined {
  return ultima.pagina + 1 < ultima.totalPaginas ? ultima.pagina + 1 : undefined;
}

export function juntarPaginas<T>(paginas: Pagina<T>[] | undefined): T[] {
  return paginas?.flatMap((pagina) => pagina.conteudo) ?? [];
}
