package br.com.canteiroconectado.estoque;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.item.TipoItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1/canteiros/{canteiroId}/estoque")
public class EstoqueController {

    private final EstoqueService servico;

    public EstoqueController(EstoqueService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Estoque do canteiro: itens ativos com saldo e status, críticos primeiro")
    @GetMapping
    public PaginaResponse<ItemEstoqueResponse> listar(
            @PathVariable UUID canteiroId,
            @RequestParam(required = false) String q,
            @Parameter(description = "Repita para combinar, ex.: status=ZERADO&status=ABAIXO_DO_MINIMO")
            @RequestParam(required = false) List<StatusEstoque> status,
            @RequestParam(required = false) TipoItem tipo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(canteiroId, q, status, tipo, pagina, tamanho);
    }
}
