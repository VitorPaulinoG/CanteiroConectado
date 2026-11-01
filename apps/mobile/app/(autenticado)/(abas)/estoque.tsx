import { EmptyState, TelaComCabecalho } from '@/shared';

export default function Estoque() {
  return (
    <TelaComCabecalho titulo="Estoque">
      <EmptyState title="Estoque do canteiro" description="O saldo dos itens aparece aqui." />
    </TelaComCabecalho>
  );
}
