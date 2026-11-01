import { EmptyState } from '@/shared';
import { TelaDoCanteiro } from '@/shared/canteiro';
import { useUsuario } from '@/shared/sessao';

export function InicioTela() {
  const usuario = useUsuario();
  const primeiroNome = usuario.nome.split(' ')[0];

  return (
    <TelaDoCanteiro titulo={`Olá, ${primeiroNome}`}>
      <EmptyState
        title="Tudo certo por aqui"
        description="Os alertas de estoque e as requisições aparecem nesta tela."
      />
    </TelaDoCanteiro>
  );
}
