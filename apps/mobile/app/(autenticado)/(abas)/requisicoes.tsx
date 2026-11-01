import { EmptyState, TelaComCabecalho } from '@/shared';

export default function Requisicoes() {
  return (
    <TelaComCabecalho titulo="Requisições">
      <EmptyState title="Requisições" description="Suas requisições e as que esperam decisão." />
    </TelaComCabecalho>
  );
}
