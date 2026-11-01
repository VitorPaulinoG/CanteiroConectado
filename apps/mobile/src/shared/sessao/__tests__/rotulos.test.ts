import { iniciais, rotuloDoPerfil } from '../rotulos';

describe('rotulos', () => {
  it('monta as iniciais do primeiro e do último nome', () => {
    expect(iniciais('Sebastião da Silva Lima')).toBe('SL');
    expect(iniciais('camila')).toBe('C');
    expect(iniciais('  ')).toBe('?');
  });

  it('descreve o perfil', () => {
    expect(rotuloDoPerfil({ perfil: 'ALMOXARIFE', administrador: false })).toBe('Almoxarife');
    expect(rotuloDoPerfil({ perfil: 'GESTOR', administrador: true })).toBe(
      'Gestor · Administrador',
    );
  });
});
