package br.com.canteiroconectado.requisicao;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.PaginaResponse;
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

    @Operation(summary = "Lista as requisições do canteiro: minhas ou para decidir, pendentes primeiro")
    @GetMapping
    public PaginaResponse<RequisicaoResumoResponse> listar(
            @RequestParam(required = false) UUID canteiroId,
            @RequestParam(required = false) String escopo,
            @RequestParam(required = false) List<StatusRequisicao> status,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(canteiroId, escopo, status, pagina, tamanho);
    }

    @Operation(summary = "Requisição completa com itens, linha do tempo e ações permitidas")
    @GetMapping("/{id}")
    public RequisicaoResponse buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }
}
