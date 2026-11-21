import { validarApiUrl } from '../env';

describe('validarApiUrl', () => {
  it('recusa valor ausente', () => {
    expect(validarApiUrl(undefined)).toEqual({
      valido: false,
      problema: 'A variável EXPO_PUBLIC_API_URL não foi definida.',
    });
  });

  it('recusa valor em branco', () => {
    expect(validarApiUrl('   ').valido).toBe(false);
  });

  it('recusa endereço sem http ou https', () => {
    const resultado = validarApiUrl('10.0.2.2:8080');
    expect(resultado.valido).toBe(false);
  });

  it.each([
    ['http://10.0.2.2:8080', 'http://10.0.2.2:8080'],
    ['http://192.168.0.10:8080/', 'http://192.168.0.10:8080'],
    ['https://canteiro.onrender.com', 'https://canteiro.onrender.com'],
  ])('aceita %s', (valor, esperado) => {
    expect(validarApiUrl(valor)).toEqual({ valido: true, apiUrl: esperado });
  });
});
