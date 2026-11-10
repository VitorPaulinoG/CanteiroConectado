package br.com.canteiroconectado.estoque;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.ResultadoDaGravacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
public class MovimentacaoController {

    private final RegistroDeMovimentacaoService registro;

    public MovimentacaoController(RegistroDeMovimentacaoService registro) {
        this.registro = registro;
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
