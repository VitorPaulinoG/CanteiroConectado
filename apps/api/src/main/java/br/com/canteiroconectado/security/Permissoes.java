package br.com.canteiroconectado.security;

/**
 * Expressões de {@code @PreAuthorize} usadas nos services. Os papéis vêm do JWT: o perfil vira
 * {@code ROLE_ALMOXARIFE} ou {@code ROLE_GESTOR}, e a marca de administrador vira
 * {@code ROLE_ADMIN}.
 */
public final class Permissoes {

    public static final String AUTENTICADO = "isAuthenticated()";
    public static final String ALMOXARIFE = "hasRole('ALMOXARIFE')";
    public static final String GESTOR = "hasRole('GESTOR')";
    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String ALMOXARIFE_OU_GESTOR = "hasAnyRole('ALMOXARIFE', 'GESTOR')";

    private Permissoes() {
    }
}
