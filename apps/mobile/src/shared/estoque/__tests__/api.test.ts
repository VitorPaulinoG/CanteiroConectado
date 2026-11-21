import { api } from '@/shared/api';

import { listarEstoque } from '../api';
import { seloDoStatus } from '../status';

describe('listarEstoque', () => {
  it('repete o parâmetro status para combinar filtros', async () => {
    let url = '';
    const espiao = jest.spyOn(api, 'get').mockImplementation(async (endereco, config) => {
      url = api.getUri({ url: endereco, ...config });
      return { data: { conteudo: [], pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 } };
    });

    await listarEstoque('c1', { status: ['ZERADO', 'ABAIXO_DO_MINIMO'] }, 2);

    expect(url).toContain('/api/v1/canteiros/c1/estoque?');
    expect(url).toContain('status=ZERADO&status=ABAIXO_DO_MINIMO');
    expect(url).toContain('pagina=2');
    espiao.mockRestore();
  });
});

describe('seloDoStatus', () => {
  it('traduz o status da API para o selo', () => {
    expect(seloDoStatus('ZERADO')).toBe('empty');
    expect(seloDoStatus('ABAIXO_DO_MINIMO')).toBe('belowMinimum');
    expect(seloDoStatus('NORMAL')).toBe('normal');
  });
});
