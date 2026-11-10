export const MENSAGENS_POR_CODIGO: Record<string, string> = {
  SEM_CONEXAO: 'Sem conexão com o servidor. Confira a internet e tente de novo.',
  TEMPO_ESGOTADO: 'O servidor demorou para responder. Tente de novo.',
  VALIDACAO_INVALIDA: 'Confira os campos e tente de novo.',
  NAO_AUTENTICADO: 'Entre de novo para continuar.',
  ACESSO_NEGADO: 'Você não tem permissão para esta ação.',
  RECURSO_NAO_ENCONTRADO: 'O registro não foi encontrado.',
  METODO_NAO_PERMITIDO: 'Ação não disponível.',
  CONFLITO: 'A operação conflita com os dados atuais. Atualize a tela e tente de novo.',
  ERRO_INESPERADO: 'Ocorreu um erro inesperado. Tente de novo mais tarde.',
};

export const MENSAGEM_PADRAO = MENSAGENS_POR_CODIGO.ERRO_INESPERADO;

export function mensagemPara(codigo: string, detalhe?: string): string {
  return MENSAGENS_POR_CODIGO[codigo] ?? detalhe ?? MENSAGEM_PADRAO;
}
