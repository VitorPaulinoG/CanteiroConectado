package br.com.canteiroconectado.resumo;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ResumoController {

    private final ResumoService servico;

    public ResumoController(ResumoService servico) {
        this.servico = servico;
    }

    @Operation(summary = "Resumo do Início: alertas de estoque, os três mais críticos e contadores de requisição")
    @GetMapping("/api/v1/canteiros/{canteiroId}/estoque/resumo")
    public ResumoResponse resumir(@PathVariable UUID canteiroId) {
        return servico.resumir(canteiroId);
    }
}
