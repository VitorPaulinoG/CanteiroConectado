package br.com.canteiroconectado.estoque;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.ResultadoDaGravacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
public class MovimentacaoController {

    private final RegistroDeMovimentacaoService registro;
    private final HistoricoService historico;

    public MovimentacaoController(RegistroDeMovimentacaoService registro, HistoricoService historico) {
        this.registro = registro;
        this.historico = historico;
    }

    @Operation(summary = "Histórico de movimentações do canteiro, da mais recente para a mais antiga (GESTOR)")
    @GetMapping("/api/v1/movimentacoes")
    public PaginaResponse<MovimentacaoResponse> historico(
            @RequestParam(required = false) UUID canteiroId,
            @Parameter(description = "Começo do período, inclusive, em UTC (ISO-8601)")
            @RequestParam(required = false) Instant de,
            @Parameter(description = "Fim do período, exclusive, em UTC (ISO-8601)")
            @RequestParam(required = false) Instant ate,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) UUID colaboradorId,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return historico.listar(canteiroId, de, ate, tipo, itemId, colaboradorId, pagina, tamanho);
    }

    @Operation(summary = "Registra uma entrada ou saída no canteiro (ALMOXARIFE do canteiro)")
    @ApiResponse(responseCode = "201", description = "Movimentação gravada agora")
    @ApiResponse(responseCode = "200", description = "O mesmo pedido já tinha sido gravado; devolve o registro existente")
    @PostMapping("/api/v1/canteiros/{canteiroId}/movimentacoes")
    public ResponseEntity<MovimentacaoResponse> registrar(@PathVariable UUID canteiroId,
            @Valid @RequestBody MovimentacaoRequest requisicao) {
        ResultadoDaGravacao<MovimentacaoResponse> resultado = registro.registrar(canteiroId, requisicao);
        return ResponseEntity.status(resultado.novo() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.registro());
    }
}
