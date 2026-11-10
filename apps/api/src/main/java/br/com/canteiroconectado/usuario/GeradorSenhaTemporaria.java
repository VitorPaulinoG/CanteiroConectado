package br.com.canteiroconectado.usuario;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Gera senhas temporárias de 12 caracteres com letras e números, sem os caracteres que se
 * confundem na leitura ({@code 0}, {@code O}, {@code 1}, {@code l}, {@code I}). Toda senha gerada
 * tem pelo menos uma letra e um número, para atender à política de senha.
 */
@Component
public class GeradorSenhaTemporaria {

    static final int TAMANHO = 12;
    private static final String LETRAS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String NUMEROS = "23456789";
    private static final String TODOS = LETRAS + NUMEROS;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        char[] senha = new char[TAMANHO];
        senha[0] = LETRAS.charAt(aleatorio.nextInt(LETRAS.length()));
        senha[1] = NUMEROS.charAt(aleatorio.nextInt(NUMEROS.length()));
        for (int i = 2; i < TAMANHO; i++) {
            senha[i] = TODOS.charAt(aleatorio.nextInt(TODOS.length()));
        }
        for (int i = TAMANHO - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            char troca = senha[i];
            senha[i] = senha[j];
            senha[j] = troca;
        }
        return new String(senha);
    }
}
