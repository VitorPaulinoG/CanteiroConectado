import type { AcaoAuditoria } from './api';

export const ROTULO_DA_ACAO: Record<AcaoAuditoria, string> = {
  ADMIN_CRIADO: 'Administrador criado',
  LOGIN_SUCESSO: 'Login',
  LOGIN_FALHOU: 'Login com falha',
  CONTA_BLOQUEADA: 'Conta bloqueada',
  SENHA_TROCADA: 'Senha trocada',
  CANTEIRO_CRIADO: 'Canteiro criado',
  CANTEIRO_EDITADO: 'Canteiro editado',
  USUARIO_CRIADO: 'Funcionário criado',
  USUARIO_EDITADO: 'Funcionário editado',
  USUARIO_DESATIVADO: 'Funcionário desativado',
  USUARIO_ATIVADO: 'Funcionário reativado',
  SENHA_REDEFINIDA: 'Senha redefinida',
  CREDENCIAL_CONSULTADA: 'Credencial inicial consultada',
  COLABORADOR_CRIADO: 'Colaborador criado',
  COLABORADOR_EDITADO: 'Colaborador editado',
  ITEM_CRIADO: 'Item criado',
  ITEM_EDITADO: 'Item editado',
  REQUISICAO_CRIADA: 'Requisição criada',
  REQUISICAO_APROVADA: 'Requisição aprovada',
  REQUISICAO_RECUSADA: 'Requisição recusada',
  REQUISICAO_ATENDIDA: 'Requisição atendida',
  REQUISICAO_CANCELADA: 'Requisição cancelada',
  MOVIMENTACAO_ESTORNADA: 'Movimentação estornada',
};

export const ICONE_DA_ACAO: Partial<Record<AcaoAuditoria, string>> = {
  LOGIN_SUCESSO: 'login',
  LOGIN_FALHOU: 'account-alert-outline',
  CONTA_BLOQUEADA: 'lock-alert-outline',
  SENHA_TROCADA: 'lock-reset',
  SENHA_REDEFINIDA: 'lock-reset',
  CREDENCIAL_CONSULTADA: 'key-outline',
  MOVIMENTACAO_ESTORNADA: 'undo-variant',
};

export const ROTULO_DA_ENTIDADE: Record<string, string> = {
  usuario: 'Funcionário',
  canteiro: 'Canteiro',
  item: 'Item',
  colaborador: 'Colaborador',
  requisicao: 'Requisição',
  movimentacao: 'Movimentação',
};

export const GRUPOS_DE_ACAO: { rotulo: string; acoes: AcaoAuditoria[] }[] = [
  {
    rotulo: 'Acesso',
    acoes: ['LOGIN_SUCESSO', 'LOGIN_FALHOU', 'CONTA_BLOQUEADA', 'SENHA_TROCADA'],
  },
  {
    rotulo: 'Funcionários',
    acoes: [
      'ADMIN_CRIADO',
      'USUARIO_CRIADO',
      'USUARIO_EDITADO',
      'USUARIO_DESATIVADO',
      'USUARIO_ATIVADO',
      'SENHA_REDEFINIDA',
      'CREDENCIAL_CONSULTADA',
    ],
  },
  {
    rotulo: 'Cadastros',
    acoes: [
      'CANTEIRO_CRIADO',
      'CANTEIRO_EDITADO',
      'ITEM_CRIADO',
      'ITEM_EDITADO',
      'COLABORADOR_CRIADO',
      'COLABORADOR_EDITADO',
    ],
  },
  {
    rotulo: 'Operação',
    acoes: [
      'MOVIMENTACAO_ESTORNADA',
      'REQUISICAO_CRIADA',
      'REQUISICAO_APROVADA',
      'REQUISICAO_RECUSADA',
      'REQUISICAO_ATENDIDA',
      'REQUISICAO_CANCELADA',
    ],
  },
];

const ROTULO_DO_DETALHE: Record<string, string> = {
  email: 'E-mail',
  motivo: 'Motivo',
  perfil: 'Perfil',
  canteiroId: 'Canteiro',
  nome: 'Nome',
  numero: 'Número',
  codigo: 'Código',
  descricao: 'Descrição',
  ativo: 'Ativo',
  bloqueadoAte: 'Bloqueado até',
  tentativas: 'Tentativas',
  primeiroAcesso: 'Primeiro acesso',
  previsaoChegada: 'Previsão de chegada',
  quantidade: 'Quantidade',
  tipoOriginal: 'Tipo original',
  estornoId: 'Estorno',
  itemId: 'Item',
  itens: 'Itens',
  endereco: 'Endereço',
  quantidadeMinima: 'Quantidade mínima',
  unidade: 'Unidade',
  tipo: 'Tipo',
};

export function rotuloDoDetalhe(chave: string): string {
  return ROTULO_DO_DETALHE[chave] ?? chave;
}

export function textoDoDetalhe(valor: unknown): string {
  if (typeof valor === 'boolean') {
    return valor ? 'Sim' : 'Não';
  }
  if (valor === null || valor === undefined || valor === '') {
    return '—';
  }
  return String(valor);
}
