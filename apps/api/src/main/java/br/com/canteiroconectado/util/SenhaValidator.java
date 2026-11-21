package br.com.canteiroconectado.util;

import java.util.Optional;

public final class SenhaValidator {

    public static final int TAMANHO_MINIMO = 8;

    private SenhaValidator() {
    }

    public static Optional<String> problema(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            return Optional.of("A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        if (senha.chars().noneMatch(Character::isLetter)) {
            return Optional.of("A senha deve ter pelo menos uma letra.");
        }
        if (senha.chars().noneMatch(Character::isDigit)) {
            return Optional.of("A senha deve ter pelo menos um número.");
        }
        return Optional.empty();
    }
}
