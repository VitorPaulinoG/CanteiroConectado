package br.com.canteiroconectado.security;

public final class Permissoes {

    public static final String AUTENTICADO = "isAuthenticated()";
    public static final String ALMOXARIFE = "hasRole('ALMOXARIFE')";
    public static final String GESTOR = "hasRole('GESTOR')";
    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String ALMOXARIFE_OU_GESTOR = "hasAnyRole('ALMOXARIFE', 'GESTOR')";

    private Permissoes() {
    }
}
