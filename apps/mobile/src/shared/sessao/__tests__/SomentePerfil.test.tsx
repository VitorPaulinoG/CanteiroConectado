import { render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';

import { administrador, almoxarife, autenticado, gestor, valorSessao } from '@/test-utils/sessao';

import { SessaoContext } from '../SessaoContext';
import { permite, SomentePerfil } from '../SomentePerfil';
import type { EstadoSessao } from '../tipos';

function montar(estado: EstadoSessao, props: Parameters<typeof SomentePerfil>[0]) {
  return render(
    <SessaoContext.Provider value={valorSessao(estado)}>
      <SomentePerfil {...props} />
    </SessaoContext.Provider>,
  );
}

describe('SomentePerfil', () => {
  it('mostra o conteúdo para o perfil permitido', async () => {
    await montar(autenticado(almoxarife), {
      perfis: ['ALMOXARIFE'],
      children: <Text>registrar saída</Text>,
    });
    expect(screen.getByText('registrar saída')).toBeTruthy();
  });

  it('esconde o conteúdo e mostra a alternativa para outro perfil', async () => {
    await montar(autenticado(gestor), {
      perfis: ['ALMOXARIFE'],
      children: <Text>registrar saída</Text>,
      alternativa: <Text>sem acesso</Text>,
    });
    expect(screen.queryByText('registrar saída')).toBeNull();
    expect(screen.getByText('sem acesso')).toBeTruthy();
  });

  it('exige a marca de administrador quando pedida', () => {
    expect(permite(gestor, { administrador: true })).toBe(false);
    expect(permite(administrador, { administrador: true })).toBe(true);
    expect(permite(administrador, { perfis: ['ALMOXARIFE'], administrador: true })).toBe(false);
  });

  it('esconde o conteúdo fora da área autenticada', async () => {
    await montar({ status: 'anonimo' }, { children: <Text>qualquer</Text> });
    expect(screen.queryByText('qualquer')).toBeNull();
  });
});
