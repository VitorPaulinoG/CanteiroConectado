import { AcessoNegado, EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';
import { SomentePerfil } from '@/shared/sessao';

export default function Movimentar() {
  return (
    <TelaDoCanteiro titulo="Movimentar">
      <SomentePerfil perfis={['ALMOXARIFE']} alternativa={<AcessoNegado />}>
        <EmptyState title="Movimentar" description="Registre entradas e saídas por aqui." />
      </SomentePerfil>
    </TelaDoCanteiro>
  );
}
