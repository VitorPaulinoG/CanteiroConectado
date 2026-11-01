import { act, fireEvent, screen, waitFor } from '@testing-library/react-native';
import { useState } from 'react';

import { erroDaApi } from '@/test-utils/api';
import { renderizar } from '@/test-utils/render';
import { almoxarife, autenticado, ComSessao, valorSessao } from '@/test-utils/sessao';

import { criarColaborador, listarColaboradores, type Colaborador } from '../api';
import { SeletorColaborador } from '../SeletorColaborador';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('../api', () => ({
  ...jest.requireActual('../api'),
  listarColaboradores: jest.fn(),
  criarColaborador: jest.fn(),
}));

const joao: Colaborador = {
  id: 'col-1',
  nome: 'João da Silva',
  canteiroId: 'c-aurora',
  ativo: true,
};

function pagina(conteudo: Colaborador[]) {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}

const aoMudar = jest.fn();

function Campo() {
  const [valor, setValor] = useState<Colaborador | null>(null);
  return (
    <SeletorColaborador
      canteiroId="c-aurora"
      valor={valor}
      aoMudar={(novo) => {
        aoMudar(novo);
        setValor(novo);
      }}
    />
  );
}

function montar() {
  return renderizar(
    <ComSessao valor={valorSessao(autenticado(almoxarife))}>
      <Campo />
    </ComSessao>,
  );
}

describe('SeletorColaborador (M2)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
  });
  afterEach(() => jest.useRealTimers());

  it('busca só colaboradores ativos do canteiro e escolhe um', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([joao]));
    await montar();

    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));
    await fireEvent.press(await screen.findByText('João da Silva'));

    expect(listarColaboradores).toHaveBeenCalledWith('c-aurora', { q: undefined, ativo: true }, 0);
    expect(aoMudar).toHaveBeenCalledWith(joao);
    expect(screen.getByDisplayValue('João da Silva')).toBeTruthy();
  });

  it('filtra depois da pausa na digitação', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([]));
    await montar();
    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));

    await fireEvent.changeText(screen.getByLabelText('Buscar colaborador'), 'jo');
    await act(async () => {
      jest.advanceTimersByTime(300);
    });

    expect(listarColaboradores).toHaveBeenLastCalledWith('c-aurora', { q: 'jo', ativo: true }, 0);
  });

  it('cadastra um colaborador novo sem sair da tela e já o escolhe', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([]));
    const maria = { ...joao, id: 'col-2', nome: 'Maria Souza' };
    jest.mocked(criarColaborador).mockResolvedValue(maria);
    await montar();
    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));

    await fireEvent.changeText(screen.getByLabelText('Buscar colaborador'), 'Maria Souza');
    await fireEvent.press(screen.getByText('Novo colaborador: Maria Souza'));
    expect(screen.getByLabelText('Nome do colaborador').props.value).toBe('Maria Souza');
    await fireEvent.press(screen.getByText('Salvar'));

    await waitFor(() => expect(aoMudar).toHaveBeenCalledWith(maria));
    expect(criarColaborador).toHaveBeenCalledWith('Maria Souza', 'c-aurora');
  });

  it('avisa no campo quando o nome já existe', async () => {
    jest.mocked(listarColaboradores).mockResolvedValue(pagina([]));
    jest.mocked(criarColaborador).mockRejectedValue(
      erroDaApi(409, {
        codigo: 'COLABORADOR_DUPLICADO',
        detail: 'Já existe um colaborador com este nome neste canteiro.',
        campo: 'nome',
      }),
    );
    await montar();
    await fireEvent.press(screen.getByLabelText('Escolher colaborador'));
    await fireEvent.press(screen.getByText('Novo colaborador'));
    await fireEvent.changeText(screen.getByLabelText('Nome do colaborador'), 'João da Silva');
    await fireEvent.press(screen.getByText('Salvar'));

    expect(
      await screen.findByText('Já existe um colaborador com este nome neste canteiro.'),
    ).toBeTruthy();
    expect(aoMudar).not.toHaveBeenCalled();
  });
});
