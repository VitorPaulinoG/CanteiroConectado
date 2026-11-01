package br.com.canteiroconectado.colaborador;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/colaboradores")
public class ColaboradorController {

    private final ColaboradorService servico;

    public ColaboradorController(ColaboradorService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Lista os colaboradores do canteiro com busca por nome")
    @GetMapping
    public PaginaResponse<ColaboradorResponse> listar(
            @RequestParam(required = false) UUID canteiroId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(canteiroId, q, ativo, pagina, tamanho);
    }

    @Operation(summary = "Detalhe de um colaborador")
    @GetMapping("/{id}")
    public ColaboradorResponse buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Cadastra um colaborador no canteiro")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaboradorResponse criar(@Valid @RequestBody ColaboradorRequest requisicao) {
        return servico.criar(requisicao);
    }

    @Operation(summary = "Renomeia, desativa ou reativa um colaborador")
    @PatchMapping("/{id}")
    public ColaboradorResponse atualizar(@PathVariable UUID id,
            @Valid @RequestBody ColaboradorAtualizacaoRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }
}
