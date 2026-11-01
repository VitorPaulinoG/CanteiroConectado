export type Ambiente =
  | { valido: true; apiUrl: string }
  | { valido: false; problema: string };

export const VARIAVEL_API_URL = 'EXPO_PUBLIC_API_URL';

export function validarApiUrl(valor: string | undefined): Ambiente {
  const texto = valor?.trim();
  if (!texto) {
    return {
      valido: false,
      problema: `A variável ${VARIAVEL_API_URL} não foi definida.`,
    };
  }
  if (!/^https?:\/\/[^\s/]+(:\d+)?(\/[^\s]*)?$/i.test(texto)) {
    return {
      valido: false,
      problema: `A variável ${VARIAVEL_API_URL} deve começar com http:// ou https:// (valor atual: ${texto}).`,
    };
  }
  return { valido: true, apiUrl: texto.replace(/\/+$/, '') };
}

export const ambiente: Ambiente = validarApiUrl(process.env.EXPO_PUBLIC_API_URL);
