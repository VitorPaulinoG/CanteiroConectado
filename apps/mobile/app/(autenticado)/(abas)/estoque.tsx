import { EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';

export default function Estoque() {
  return (
    <TelaDoCanteiro titulo="Estoque">
      <EmptyState title="Estoque do canteiro" description="O saldo dos itens aparece aqui." />
    </TelaDoCanteiro>
  );
}
