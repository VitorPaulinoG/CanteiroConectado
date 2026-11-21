package br.com.canteiroconectado.autenticacao;

import java.util.Optional;

/**
 * Regras de senha do MVP: 8 caracteres ou mais, com pelo menos uma letra e um número.
 */
public final class PoliticaDeSenha {

    public static final int TAMANHO_MINIMO = 8;

    private PoliticaDeSenha() {
    }

    /**
     * Confere a senha e devolve o motivo da recusa, ou vazio quando ela atende às regras.
     */
    public static Optional<String> problemaDa(String senha) {
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
