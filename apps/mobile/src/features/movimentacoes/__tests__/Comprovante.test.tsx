import { fireEvent, screen } from '@testing-library/react-native';

import { AppError, type Envio } from '@/shared/api';
import type { Movimentacao } from '@/shared/estoque';
import { saidaGravada } from '@/test-utils/estoque';
import { renderizar } from '@/test-utils/render';

import type { PedidoDeMovimentacao } from '../api';
import { Comprovante, type ResumoDoPedido } from '../componentes/Comprovante';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');

const resumo: ResumoDoPedido = {
  titulo: 'Saída de ferramenta',
  itemDescricao: 'Furadeira de impacto 700W',
  itemCodigo: 'FER-0012',
  unidadeSimbolo: 'un',
  motivo: 'RETIRADA',
  quantidade: 1,
  colaboradorNome: 'João',
  frenteObra: 'Fundação',
};

function envio(
  parcial: Partial<Envio<PedidoDeMovimentacao, Movimentacao>>,
): Envio<PedidoDeMovimentacao, Movimentacao> {
  return {
    estado: 'parado',
    enviando: false,
    salvo: false,
    falhou: false,
    erro: null,
    semResposta: false,
    resposta: undefined,
    enviar: jest.fn(),
    tentarDeNovo: jest.fn(),
    reiniciar: jest.fn(),
    ...parcial,
  };
}

function montar(estado: Envio<PedidoDeMovimentacao, Movimentacao>) {
  const acoes = { aoCorrigir: jest.fn(), aoNovoRegistro: jest.fn(), aoConcluir: jest.fn() };
  return {
    acoes,
    tela: renderizar(
      <Comprovante resumo={resumo} envio={estado} rotuloNovoRegistro="Nova saída" {...acoes} />,
    ),
  };
}

describe('Comprovante (S11)', () => {
  it('mostra o pedido enquanto envia, avisa que ainda não vale e não oferece concluir', async () => {
    await montar(envio({ estado: 'enviando', enviando: true })).tela;

    expect(screen.getByText('Enviando…')).toBeTruthy();
    expect(screen.getByText('Furadeira de impacto 700W (FER-0012)')).toBeTruthy();
    expect(screen.getByText('Retirada')).toBeTruthy();
    expect(screen.getByText(/O registro só vale depois de aparecer/)).toBeTruthy();
    expect(screen.queryByText('Concluir')).toBeNull();
    expect(screen.queryByText('Saldo depois')).toBeNull();
  });

  it('mostra os dados do servidor quando salvo e libera novo registro e concluir', async () => {
    const { acoes, tela } = montar(envio({ estado: 'salvo', salvo: true, resposta: saidaGravada }));
    await tela;

    expect(screen.getByText('Salvo no servidor')).toBeTruthy();
    expect(screen.getByText('João da Silva')).toBeTruthy();
    expect(screen.queryByText('João')).toBeNull();
    expect(screen.getByText('Saldo depois')).toBeTruthy();
    expect(screen.getByText('Sebastião Lima')).toBeTruthy();
    expect(screen.queryByText(/O registro só vale depois de aparecer/)).toBeNull();

    await fireEvent.press(screen.getByText('Nova saída'));
    await fireEvent.press(screen.getByText('Concluir'));
    expect(acoes.aoNovoRegistro).toHaveBeenCalledTimes(1);
    expect(acoes.aoConcluir).toHaveBeenCalledTimes(1);
  });

  it('oferece Tentar de novo quando não houve resposta', async () => {
    const estado = envio({
      estado: 'falhou',
      falhou: true,
      semResposta: true,
      erro: new AppError({ codigo: 'SEM_CONEXAO', mensagem: 'Sem conexão.', semResposta: true }),
    });
    const { acoes, tela } = montar(estado);
    await tela;

    expect(screen.getByText('Não foi possível enviar. Sem conexão.')).toBeTruthy();
    await fireEvent.press(screen.getByText('Tentar de novo'));
    expect(estado.tentarDeNovo).toHaveBeenCalledTimes(1);
    expect(acoes.aoCorrigir).not.toHaveBeenCalled();
  });

  it('oferece Corrigir quando o servidor recusou', async () => {
    const estado = envio({
      estado: 'falhou',
      falhou: true,
      erro: new AppError({ codigo: 'COLABORADOR_INVALIDO', mensagem: 'Colaborador desativado.' }),
    });
    const { acoes, tela } = montar(estado);
    await tela;

    expect(screen.getByText('Colaborador desativado.')).toBeTruthy();
    expect(screen.queryByText('Tentar de novo')).toBeNull();
    await fireEvent.press(screen.getByText('Corrigir'));
    expect(acoes.aoCorrigir).toHaveBeenCalledTimes(1);
  });
});
