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
  CREDENCIAIS_INVALIDAS: 'E-mail ou senha incorretos.',
  USUARIO_INATIVO: 'Este usuário está desativado. Fale com o administrador.',
  CONTA_BLOQUEADA: 'Conta bloqueada por excesso de tentativas.',
  TROCA_DE_SENHA_OBRIGATORIA: 'Troque a senha temporária para continuar.',
  SENHA_ATUAL_INCORRETA: 'A senha atual está incorreta.',
  SENHA_IGUAL_A_ATUAL: 'A nova senha deve ser diferente da atual.',
  CANTEIRO_DUPLICADO: 'Já existe um canteiro com este nome.',
  CANTEIRO_INATIVO: 'Este canteiro está desativado.',
  EMAIL_JA_CADASTRADO: 'Já existe um funcionário com este e-mail.',
  CANTEIRO_OBRIGATORIO: 'Escolha o canteiro do almoxarife.',
  AUTODESATIVACAO_PROIBIDA: 'Você não pode desativar a si mesmo.',
  ADMINISTRADOR_PROTEGIDO: 'O perfil do administrador não pode ser alterado.',
  CREDENCIAL_JA_TROCADA: 'O funcionário já trocou a senha temporária.',
  COLABORADOR_DUPLICADO: 'Já existe um colaborador com este nome neste canteiro.',
};

export const MENSAGEM_PADRAO = MENSAGENS_POR_CODIGO.ERRO_INESPERADO;

export function mensagemPara(codigo: string, detalhe?: string): string {
  return MENSAGENS_POR_CODIGO[codigo] ?? detalhe ?? MENSAGEM_PADRAO;
}
