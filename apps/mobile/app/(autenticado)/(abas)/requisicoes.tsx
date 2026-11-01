import { EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';

export default function Requisicoes() {
  return (
    <TelaDoCanteiro titulo="Requisições">
      <EmptyState title="Requisições" description="Suas requisições e as que esperam decisão." />
    </TelaDoCanteiro>
  );
}
