import { renderRouter, screen } from 'expo-router/testing-library';
import { Text } from 'react-native';

import { SessaoContext, type EstadoSessao, type ValorSessao } from '@/shared/sessao';

import { Navegacao } from '../../app/_layout';
import Abertura from '../../app/index';
import AreaAutenticadaLayout from '../../app/(autenticado)/_layout';

const usuario = {
  id: '1',
  nome: 'Sebastião',
  email: 's@obra.com',
  perfil: 'ALMOXARIFE' as const,
  administrador: false,
  canteiroId: 'c1',
  precisaTrocarSenha: false,
};

function rotas(estado: EstadoSessao) {
  const valor: ValorSessao = {
    estado,
    entrar: jest.fn(),
    sair: jest.fn(),
    atualizarUsuario: jest.fn(),
    adiarTroca: jest.fn(),
  };
  return {
    _layout: () => (
      <SessaoContext.Provider value={valor}>
        <Navegacao />
      </SessaoContext.Provider>
    ),
    index: Abertura,
    '(autenticacao)/login': () => <Text>tela de login</Text>,
    '(autenticacao)/trocar-senha': () => <Text>tela de troca de senha</Text>,
    '(autenticado)/_layout': AreaAutenticadaLayout,
    '(autenticado)/inicio': () => <Text>tela de início</Text>,
    sobre: () => <Text>sobre</Text>,
  };
}

describe('Navegação por sessão (S01)', () => {
  it('leva o usuário anônimo ao login', async () => {
    await renderRouter(rotas({ status: 'anonimo' }));
    expect(await screen.findByText('tela de login')).toBeTruthy();
  });

  it('leva o usuário autenticado ao início', async () => {
    await renderRouter(rotas({ status: 'autenticado', usuario, trocaAdiada: false }));
    expect(await screen.findByText('tela de início')).toBeTruthy();
  });

  it('não deixa o anônimo abrir a área autenticada', async () => {
    await renderRouter(rotas({ status: 'anonimo' }), { initialUrl: '/inicio' });
    expect(await screen.findByText('tela de login')).toBeTruthy();
    expect(screen.queryByText('tela de início')).toBeNull();
  });

  it('leva à troca de senha quando ela é obrigatória', async () => {
    await renderRouter(
      rotas({
        status: 'autenticado',
        usuario: { ...usuario, precisaTrocarSenha: true },
        trocaAdiada: false,
      }),
    );
    expect(await screen.findByText('tela de troca de senha')).toBeTruthy();
  });

  it('não deixa abrir a área autenticada com a troca pendente', async () => {
    await renderRouter(
      rotas({
        status: 'autenticado',
        usuario: { ...usuario, precisaTrocarSenha: true },
        trocaAdiada: false,
      }),
      { initialUrl: '/inicio' },
    );
    expect(await screen.findByText('tela de troca de senha')).toBeTruthy();
    expect(screen.queryByText('tela de início')).toBeNull();
  });

  it('leva ao início quando o administrador adiou a troca', async () => {
    await renderRouter(
      rotas({
        status: 'autenticado',
        usuario: { ...usuario, precisaTrocarSenha: true },
        trocaAdiada: true,
      }),
    );
    expect(await screen.findByText('tela de início')).toBeTruthy();
  });
});
