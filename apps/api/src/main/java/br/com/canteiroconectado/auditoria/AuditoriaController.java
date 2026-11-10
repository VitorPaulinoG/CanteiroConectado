package br.com.canteiroconectado.auditoria;

import java.time.Instant;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.canteiroconectado.comum.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditoriaService servico;

    public AuditoriaController(AuditoriaService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Trilha de auditoria, do evento mais recente para o mais antigo (GESTOR, somente leitura)")
    @GetMapping
    public PaginaResponse<EventoDeAuditoriaResponse> listar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) AcaoAuditoria acao,
            @Parameter(description = "Começo do período, inclusive, em UTC (ISO-8601)")
            @RequestParam(required = false) Instant de,
            @Parameter(description = "Fim do período, exclusive, em UTC (ISO-8601)")
            @RequestParam(required = false) Instant ate,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return servico.listar(usuarioId, acao, de, ate, pagina, tamanho);
    }
}
