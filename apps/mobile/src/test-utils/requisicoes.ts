import type { Requisicao, ResumoDaRequisicao } from '@/features/requisicoes/api';

export const requisicaoDaCamila: Requisicao = {
  id: 'r-1',
  numero: 'REQ-0001',
  canteiroId: 'c-aurora',
  canteiroNome: 'Residencial Aurora',
  status: 'PENDENTE',
  solicitanteId: 'u-gestor',
  solicitanteNome: 'Camila Souza',
  solicitantePerfil: 'GESTOR',
  frenteObra: 'Laje do 3º andar',
  observacao: 'Urgente',
  necessarioAte: '2026-11-10',
  previsaoChegada: null,
  criadoEm: '2026-11-01T15:00:00Z',
  decididoPorId: null,
  decididoPorNome: null,
  decididoEm: null,
  observacaoDecisao: null,
  itens: [
    {
      itemId: 'i-cimento',
      codigo: 'MAT-0001',
      descricao: 'Cimento CP-II 50kg',
      unidadeSimbolo: 'saco',
      quantidade: 40,
    },
  ],
  historico: [
    {
      status: 'PENDENTE',
      usuarioId: 'u-gestor',
      usuarioNome: 'Camila Souza',
      observacao: null,
      criadoEm: '2026-11-01T15:00:00Z',
    },
  ],
  acoes: { decidir: false, atender: false, cancelar: true },
};

export function resumoDe(requisicao: Requisicao): ResumoDaRequisicao {
  const {
    canteiroNome: _canteiroNome,
    observacao: _observacao,
    decididoPorId: _decididoPorId,
    decididoPorNome: _decididoPorNome,
    decididoEm: _decididoEm,
    observacaoDecisao: _observacaoDecisao,
    historico: _historico,
    acoes: _acoes,
    ...resumo
  } = requisicao;
  return resumo;
}
