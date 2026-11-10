import { AcessoNegado, EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';
import { SomentePerfil } from '@/shared/sessao';

export default function Historico() {
  return (
    <TelaDoCanteiro titulo="Histórico">
      <SomentePerfil perfis={['GESTOR']} alternativa={<AcessoNegado />}>
        <EmptyState title="Histórico" description="As movimentações do canteiro aparecem aqui." />
      </SomentePerfil>
    </TelaDoCanteiro>
  );
}
