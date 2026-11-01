import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { listarCanteiros } from '@/shared/canteiro/api';
import { renderizar } from '@/test-utils/render';
import {
  administrador,
  aurora,
  autenticado,
  ComSessao,
  gestor,
  valorCanteiroAtivo,
  valorSessao,
} from '@/test-utils/sessao';

import { EscolherCanteiroTela } from '../telas/EscolherCanteiroTela';

jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => 'MaterialCommunityIcons');
jest.mock('@/shared/canteiro/api', () => ({
  ...jest.requireActual('@/shared/canteiro/api'),
  listarCanteiros: jest.fn(),
}));

const mockReplace = jest.fn();
const mockBack = jest.fn();
const mockPush = jest.fn();
let mockPodeVoltar = false;
jest.mock('expo-router', () => ({
  useRouter: () => ({
    replace: mockReplace,
    back: mockBack,
    push: mockPush,
    canGoBack: () => mockPodeVoltar,
  }),
}));

const sol = { id: 'c-sol', nome: 'Edifício Sol', endereco: null, ativo: true };

describe('EscolherCanteiroTela (S05)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPodeVoltar = false;
  });

  it('escolhe o canteiro e abre o início quando a escolha é obrigatória', async () => {
    jest.mocked(listarCanteiros).mockResolvedValue([aurora, sol]);
    const canteiro = valorCanteiroAtivo({
      status: 'escolher',
      canteiroId: null,
      canteiro: undefined,
      podeEscolher: true,
    });
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))} canteiro={canteiro}>
        <EscolherCanteiroTela />
      </ComSessao>,
    );

    expect(await screen.findByText('Edifício Sol')).toBeTruthy();
    expect(screen.getByText('Usar este canteiro')).toBeDisabled();
    expect(screen.queryByLabelText('Voltar')).toBeNull();

    await fireEvent.press(screen.getByText('Edifício Sol'));
    await fireEvent.press(screen.getByText('Usar este canteiro'));

    await waitFor(() => expect(canteiro.escolher).toHaveBeenCalledWith('c-sol'));
    expect(mockReplace).toHaveBeenCalledWith('/inicio');
  });

  it('volta para a tela anterior quando o GESTOR troca de canteiro', async () => {
    mockPodeVoltar = true;
    jest.mocked(listarCanteiros).mockResolvedValue([aurora, sol]);
    const canteiro = valorCanteiroAtivo({ podeEscolher: true });
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(gestor))} canteiro={canteiro}>
        <EscolherCanteiroTela />
      </ComSessao>,
    );

    await fireEvent.press(await screen.findByText('Edifício Sol'));
    await fireEvent.press(screen.getByText('Usar este canteiro'));

    await waitFor(() => expect(mockBack).toHaveBeenCalled());
  });

  it('oferece cadastrar canteiro ao administrador quando não há nenhum', async () => {
    jest.mocked(listarCanteiros).mockResolvedValue([]);
    const canteiro = valorCanteiroAtivo({
      status: 'escolher',
      canteiroId: null,
      canteiro: undefined,
    });
    await renderizar(
      <ComSessao valor={valorSessao(autenticado(administrador))} canteiro={canteiro}>
        <EscolherCanteiroTela />
      </ComSessao>,
    );

    await fireEvent.press(await screen.findByText('Cadastrar canteiro'));
    expect(mockPush).toHaveBeenCalledWith('/cadastros/canteiros/novo');
  });

  it('permite sair quando a escolha é obrigatória', async () => {
    jest.mocked(listarCanteiros).mockResolvedValue([]);
    const sessao = valorSessao(autenticado(gestor));
    await renderizar(
      <ComSessao
        valor={sessao}
        canteiro={valorCanteiroAtivo({ status: 'escolher', canteiroId: null, canteiro: undefined })}
      >
        <EscolherCanteiroTela />
      </ComSessao>,
    );
    expect(
      await screen.findByText('Peça ao administrador para cadastrar um canteiro.'),
    ).toBeTruthy();
    await fireEvent.press(screen.getByLabelText('Sair'));
    expect(sessao.sair).toHaveBeenCalled();
  });
});
