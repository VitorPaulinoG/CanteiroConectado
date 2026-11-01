import * as SecureStore from 'expo-secure-store';

import { apagarSessao, CHAVE_SESSAO, lerSessao, salvarSessao, sessaoExpirada } from '../storage';

const sessao = {
  token: 'abc',
  expiraEm: '2026-11-02T00:00:00Z',
  usuario: {
    id: '1',
    nome: 'Sebastião',
    email: 'sebastiao@obra.com',
    perfil: 'ALMOXARIFE' as const,
    administrador: false,
    canteiroId: 'c1',
    precisaTrocarSenha: false,
  },
};

describe('storage da sessão', () => {
  beforeEach(() => (SecureStore as unknown as { __limpar: () => void }).__limpar());

  it('guarda e lê a sessão no SecureStore', async () => {
    await salvarSessao(sessao);
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith(CHAVE_SESSAO, JSON.stringify(sessao));
    expect(await lerSessao()).toEqual(sessao);
  });

  it('apaga a sessão', async () => {
    await salvarSessao(sessao);
    await apagarSessao();
    expect(await lerSessao()).toBeNull();
  });

  it('ignora conteúdo corrompido', async () => {
    await SecureStore.setItemAsync(CHAVE_SESSAO, '{quebrado');
    expect(await lerSessao()).toBeNull();
  });

  it('identifica sessão expirada', () => {
    expect(sessaoExpirada(sessao, Date.parse('2026-11-02T00:00:01Z'))).toBe(true);
    expect(sessaoExpirada(sessao, Date.parse('2026-11-01T23:00:00Z'))).toBe(false);
  });
});
