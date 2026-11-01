package br.com.canteiroconectado.autenticacao;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService servico;

    public AutenticacaoController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Autentica por e-mail e senha e devolve o JWT e os dados do usuário")
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return servico.login(requisicao);
    }

    @Operation(summary = "Dados do usuário autenticado")
    @GetMapping("/eu")
    public UsuarioLogadoResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return servico.buscarLogado(UUID.fromString(jwt.getSubject()));
    }
}
