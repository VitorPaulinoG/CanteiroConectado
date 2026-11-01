import { AcessoNegado, EmptyState, TelaComCabecalho } from '@/shared';
import { SomentePerfil } from '@/shared/sessao';

export default function Historico() {
  return (
    <TelaComCabecalho titulo="Histórico">
      <SomentePerfil perfis={['GESTOR']} alternativa={<AcessoNegado />}>
        <EmptyState title="Histórico" description="As movimentações do canteiro aparecem aqui." />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}
