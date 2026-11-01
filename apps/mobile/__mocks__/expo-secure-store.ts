const valores = new Map<string, string>();

export const getItemAsync = jest.fn(async (chave: string) => valores.get(chave) ?? null);
export const setItemAsync = jest.fn(async (chave: string, valor: string) => {
  valores.set(chave, valor);
});
export const deleteItemAsync = jest.fn(async (chave: string) => {
  valores.delete(chave);
});

export function __limpar() {
  valores.clear();
}
