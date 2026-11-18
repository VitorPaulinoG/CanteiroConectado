package br.com.canteiroconectado.ping;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/v1/ping")
public class PingController {

    static final String VERSAO_DESCONHECIDA = "desconhecida";

    private final String nome;
    private final String versao;
    private final Clock relogio;

    public PingController(@Value("${spring.application.name}") String nome,
            ObjectProvider<BuildProperties> build, Clock relogio) {
        this.nome = nome;
        BuildProperties propriedades = build.getIfAvailable();
        this.versao = propriedades != null ? propriedades.getVersion() : VERSAO_DESCONHECIDA;
        this.relogio = relogio;
    }

    @Operation(summary = "Confere se a API está no ar e devolve nome, versão e hora do servidor")
    @SecurityRequirements
    @GetMapping
    public PingResponse ping() {
        return new PingResponse(nome, versao, Instant.now(relogio));
    }
}
