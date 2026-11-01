package br.com.canteiroconectado.usuario;

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
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Lista funcionários com busca por nome ou e-mail (administrador)")
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(q, perfil, ativo, pagina, tamanho);
    }

    @Operation(summary = "Detalhe de um funcionário (administrador)")
    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Cria um funcionário com senha temporária (administrador)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest requisicao) {
        return servico.criar(requisicao);
    }

    @Operation(summary = "Altera nome, e-mail, perfil ou canteiro (administrador)")
    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody UsuarioAtualizacaoRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @Operation(summary = "Gera nova senha temporária e exige a troca no próximo login (administrador)")
    @PostMapping("/{id}/redefinir-senha")
    public UsuarioResponse redefinirSenha(@PathVariable UUID id) {
        return servico.redefinirSenha(id);
    }

    @Operation(summary = "Desativa o funcionário (administrador)")
    @PostMapping("/{id}/desativar")
    public UsuarioResponse desativar(@PathVariable UUID id) {
        return servico.desativar(id);
    }

    @Operation(summary = "Reativa o funcionário (administrador)")
    @PostMapping("/{id}/ativar")
    public UsuarioResponse ativar(@PathVariable UUID id) {
        return servico.ativar(id);
    }
}
