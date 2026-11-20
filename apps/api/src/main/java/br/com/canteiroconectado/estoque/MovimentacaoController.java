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
    private final EstornoService estornos;

    public MovimentacaoController(RegistroDeMovimentacaoService registro, HistoricoService historico,
            EstornoService estornos) {
        this.registro = registro;
        this.historico = historico;
        this.estornos = estornos;
    }

    @Operation(summary = "Registro completo de uma movimentação, com os vínculos de estorno")
    @GetMapping("/api/v1/movimentacoes/{id}")
    public MovimentacaoResponse buscar(@PathVariable UUID id) {
        return estornos.buscar(id);
    }

    @Operation(summary = "Estorna uma movimentação lançada por engano (ALMOXARIFE do canteiro)")
    @ApiResponse(responseCode = "201", description = "Estorno gravado agora")
    @ApiResponse(responseCode = "200", description = "O mesmo pedido já tinha sido gravado; devolve o estorno existente")
    @PostMapping("/api/v1/movimentacoes/{id}/estorno")
    public ResponseEntity<MovimentacaoResponse> estornar(@PathVariable UUID id,
            @Valid @RequestBody EstornoRequest pedido) {
        ResultadoDaGravacao<MovimentacaoResponse> resultado = estornos.estornar(id, pedido);
        return ResponseEntity.status(resultado.novo() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.registro());
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
