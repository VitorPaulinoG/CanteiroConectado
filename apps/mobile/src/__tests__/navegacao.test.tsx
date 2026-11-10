import { renderRouter, screen } from 'expo-router/testing-library';
import { PaperProvider } from 'react-native-paper';
import { Text } from 'react-native';

import { SessaoContext, type EstadoSessao, type ValorSessao } from '@/shared/sessao';
import { appTheme } from '@/theme';

import { Navegacao } from '../../app/_layout';
import Abertura from '../../app/index';
import AbasLayout from '../../app/(autenticado)/(abas)/_layout';
import AreaAutenticadaLayout from '../../app/(autenticado)/_layout';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

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
      <PaperProvider theme={appTheme}>
        <SessaoContext.Provider value={valor}>
          <Navegacao />
        </SessaoContext.Provider>
      </PaperProvider>
    ),
    index: Abertura,
    '(autenticacao)/login': () => <Text>tela de login</Text>,
    '(autenticacao)/trocar-senha': () => <Text>tela de troca de senha</Text>,
    '(autenticado)/_layout': AreaAutenticadaLayout,
    '(autenticado)/(abas)/_layout': AbasLayout,
    '(autenticado)/(abas)/inicio': () => <Text>tela de início</Text>,
    '(autenticado)/(abas)/estoque': () => <Text>tela de estoque</Text>,
    '(autenticado)/(abas)/movimentar': () => <Text>tela de movimentar</Text>,
    '(autenticado)/(abas)/requisicoes': () => <Text>tela de requisições</Text>,
    '(autenticado)/(abas)/historico': () => <Text>tela de histórico</Text>,
    '(autenticado)/(abas)/mais': () => <Text>tela mais</Text>,
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

  it('mostra Movimentar e esconde Histórico para o ALMOXARIFE', async () => {
    await renderRouter(rotas({ status: 'autenticado', usuario, trocaAdiada: false }));
    await screen.findByText('tela de início');
    for (const aba of ['Início', 'Estoque', 'Movimentar', 'Requisições', 'Mais']) {
      expect(screen.getByLabelText(aba)).toBeTruthy();
    }
    expect(screen.queryByLabelText('Histórico')).toBeNull();
  });

  it('mostra Histórico e esconde Movimentar para o GESTOR', async () => {
    await renderRouter(
      rotas({
        status: 'autenticado',
        usuario: { ...usuario, perfil: 'GESTOR', canteiroId: null },
        trocaAdiada: false,
      }),
    );
    await screen.findByText('tela de início');
    for (const aba of ['Início', 'Estoque', 'Requisições', 'Histórico', 'Mais']) {
      expect(screen.getByLabelText(aba)).toBeTruthy();
    }
    expect(screen.queryByLabelText('Movimentar')).toBeNull();
  });

  it('não deixa o GESTOR abrir Movimentar pelo endereço', async () => {
    await renderRouter(
      rotas({
        status: 'autenticado',
        usuario: { ...usuario, perfil: 'GESTOR', canteiroId: null },
        trocaAdiada: false,
      }),
      { initialUrl: '/movimentar' },
    );
    expect(await screen.findByText('tela de início')).toBeTruthy();
    expect(screen.queryByText('tela de movimentar')).toBeNull();
  });
});
