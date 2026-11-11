import { EmptyState, TelaComCabecalho } from '@/shared';
import { useUsuario } from '@/shared/sessao';

export function InicioTela() {
  const usuario = useUsuario();
  const primeiroNome = usuario.nome.split(' ')[0];

  return (
    <TelaComCabecalho titulo={`Olá, ${primeiroNome}`}>
      <EmptyState
        title="Tudo certo por aqui"
        description="Os alertas de estoque e as requisições aparecem nesta tela."
      />
    </TelaComCabecalho>
  );
}
