package br.com.canteiroconectado.item;

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
@RequestMapping("/api/v1/itens")
public class ItemController {

    private final ItemService servico;

    public ItemController(ItemService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Busca no catálogo por código ou descrição, com quem começa pelo termo primeiro")
    @GetMapping
    public PaginaResponse<ItemResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoItem tipo,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(q, tipo, ativo, pagina, tamanho);
    }

    @Operation(summary = "Detalhe de um item")
    @GetMapping("/{id}")
    public ItemResponse buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Cadastra um item (ALMOXARIFE ou GESTOR)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse criar(@Valid @RequestBody ItemRequest requisicao) {
        return servico.criar(requisicao);
    }

    @Operation(summary = "Altera um item; ativo=false desativa (ALMOXARIFE ou GESTOR)")
    @PatchMapping("/{id}")
    public ItemResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ItemAtualizacaoRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }
}
