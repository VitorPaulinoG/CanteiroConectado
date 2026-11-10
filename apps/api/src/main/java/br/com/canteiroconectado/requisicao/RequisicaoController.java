package br.com.canteiroconectado.requisicao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.ResultadoDaGravacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requisicoes")
public class RequisicaoController {

    private final RequisicaoService servico;

    public RequisicaoController(RequisicaoService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Cria uma requisição de material (ALMOXARIFE ou GESTOR)")
    @ApiResponse(responseCode = "201", description = "Requisição criada agora")
    @ApiResponse(responseCode = "200", description = "O mesmo pedido já tinha sido criado; devolve a requisição existente")
    @PostMapping
    public ResponseEntity<RequisicaoResponse> criar(@Valid @RequestBody RequisicaoRequest requisicao) {
        ResultadoDaGravacao<RequisicaoResponse> resultado = servico.criar(requisicao);
        return ResponseEntity.status(resultado.novo() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.registro());
    }
}
