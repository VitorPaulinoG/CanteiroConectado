import { fireEvent, screen } from '@testing-library/react-native';

import { cimentoNoEstoque, furadeiraNoEstoque } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, gestor, valorSessao } from '@/test-utils/sessao';
import type { UsuarioSessao } from '@/shared/sessao';

import { buscarResumo, type Resumo } from '../api';
import { InicioTela } from '../telas/InicioTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({ buscarResumo: jest.fn() }));

const mockPush = jest.fn();
const mockNavigate = jest.fn();
const mockFocos: (() => void)[] = [];
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush, navigate: mockNavigate }),
  useFocusEffect: (callback: () => void) => mockFocos.push(callback),
}));

const comAlertas: Resumo = {
  zerados: 2,
  abaixoDoMinimo: 3,
  maisCriticos: [
    { ...furadeiraNoEstoque, itemId: 'i-brita', descricao: 'Brita', saldo: 0, status: 'ZERADO' },
    { ...furadeiraNoEstoque, itemId: 'i-areia', descricao: 'Areia', saldo: 0, status: 'ZERADO' },
    cimentoNoEstoque,
  ],
  requisicoesParaDecidir: 2,
  minhasRequisicoes: { PENDENTE: 1, APROVADA: 2, RECUSADA: 0, ATENDIDA: 0, CANCELADA: 0 },
};

function montar(usuario: UsuarioSessao, resumo: Resumo = comAlertas) {
  jest.mocked(buscarResumo).mockResolvedValue(resumo);
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(usuario))}>
      <InicioTela />
    </ComSessao>,
  );
}

describe('InicioTela (S04)', () => {
  beforeEach(() => jest.clearAllMocks());

  it('mostra ao GESTOR 2 zerados, 3 abaixo do mínimo e os três mais críticos', async () => {
    await montar(gestor);

    expect(await screen.findByText('2 zerados · 3 abaixo do mínimo')).toBeTruthy();
    expect(screen.getByText('Brita')).toBeTruthy();
    expect(screen.getByText('Areia')).toBeTruthy();
    expect(screen.getByText('Cimento CP-II 50kg')).toBeTruthy();
    expect(screen.getByText('Saldo 30 saco · mínimo 50 saco')).toBeTruthy();
    expect(buscarResumo).toHaveBeenCalledWith('c-aurora');
  });

  it('abre o estoque já filtrado ao tocar no cartão de alerta', async () => {
    await montar(gestor);
    await fireEvent.press(await screen.findByLabelText('Alertas de estoque'));
    expect(mockNavigate).toHaveBeenCalledWith('/estoque?filtro=alerta');
  });

  it('mostra ao ALMOXARIFE os atalhos e 2 requisições para decidir, que abrem a aba Para decidir', async () => {
    await montar(almoxarife);

    expect(await screen.findByText('2 requisições esperando você')).toBeTruthy();
    await fireEvent.press(screen.getByText('Saída'));
    await fireEvent.press(screen.getByText('Entrada'));
    await fireEvent.press(screen.getByLabelText('Requisições para decidir'));
    expect(mockPush).toHaveBeenCalledWith('/movimentar/saida');
    expect(mockPush).toHaveBeenCalledWith('/movimentar/entrada');
    expect(mockNavigate).toHaveBeenCalledWith('/requisicoes?aba=para-decidir');
    expect(screen.queryByText('Minhas requisições')).toBeNull();
  });

  it('mostra à GESTOR as próprias requisições por status e esconde os atalhos', async () => {
    await montar(gestor);

    expect(await screen.findByText('1 pendente')).toBeTruthy();
    expect(screen.getByText('2 aprovadas')).toBeTruthy();
    expect(screen.getByText('0 recusadas')).toBeTruthy();
    expect(screen.queryByText('Saída')).toBeNull();
  });

  it('mostra Tudo certo por aqui quando não há alerta', async () => {
    await montar(almoxarife, {
      ...comAlertas,
      zerados: 0,
      abaixoDoMinimo: 0,
      maisCriticos: [],
      requisicoesParaDecidir: 0,
    });

    expect(await screen.findByText('Tudo certo por aqui')).toBeTruthy();
    expect(screen.getByText('Nenhuma requisição esperando você')).toBeTruthy();
  });

  it('atualiza ao voltar para a tela', async () => {
    await montar(gestor);
    await screen.findByText('2 zerados · 3 abaixo do mínimo');
    const aoFocar = mockFocos[mockFocos.length - 1];
    aoFocar();
    aoFocar();
    await screen.findByText('2 zerados · 3 abaixo do mínimo');
    expect(jest.mocked(buscarResumo).mock.calls.length).toBeGreaterThanOrEqual(2);
  });
});
