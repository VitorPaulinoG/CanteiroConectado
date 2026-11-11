import { AcessoNegado, EmptyState, TelaComCabecalho } from '@/shared';
import { SomentePerfil } from '@/shared/sessao';

export default function Movimentar() {
  return (
    <TelaComCabecalho titulo="Movimentar">
      <SomentePerfil perfis={['ALMOXARIFE']} alternativa={<AcessoNegado />}>
        <EmptyState title="Movimentar" description="Registre entradas e saídas por aqui." />
      </SomentePerfil>
    </TelaComCabecalho>
  );
}
