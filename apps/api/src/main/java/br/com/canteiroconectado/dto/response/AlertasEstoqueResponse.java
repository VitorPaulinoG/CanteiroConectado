package br.com.canteiroconectado.dto.response;

import java.util.List;

public record AlertasEstoqueResponse(long zerados, long abaixoMinimo, List<ItemEstoqueResponse> maisCriticos) {
}
