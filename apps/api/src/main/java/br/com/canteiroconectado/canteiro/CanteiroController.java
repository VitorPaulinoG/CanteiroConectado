package br.com.canteiroconectado.canteiro;

import java.util.List;
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

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/canteiros")
public class CanteiroController {

    private final CanteiroService servico;

    public CanteiroController(CanteiroService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Lista os canteiros que o usuário enxerga (ALMOXARIFE: só o próprio)")
    @GetMapping
    public List<CanteiroResponse> listar(@RequestParam(required = false) Boolean ativo) {
        return servico.listar(ativo);
    }

    @Operation(summary = "Detalhe de um canteiro")
    @GetMapping("/{id}")
    public CanteiroResponse buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Cria um canteiro (administrador)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CanteiroResponse criar(@Valid @RequestBody CanteiroRequest requisicao) {
        return servico.criar(requisicao);
    }

    @Operation(summary = "Altera nome, endereço ou situação de um canteiro (administrador)")
    @PatchMapping("/{id}")
    public CanteiroResponse atualizar(@PathVariable UUID id,
            @Valid @RequestBody CanteiroAtualizacaoRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }
}
