package br.com.canteiroconectado.item;

/**
 * Unidades de medida aceitas no catálogo. O app mostra {@link #getSimbolo()}.
 */
public enum Unidade {
    UN("un"),
    KG("kg"),
    M("m"),
    M2("m²"),
    M3("m³"),
    SACO("saco"),
    LATA("lata"),
    CAIXA("caixa"),
    LITRO("litro");

    private final String simbolo;

    Unidade(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }
}
