package br.com.canteiroconectado.service.resumo;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import br.com.canteiroconectado.dto.response.AlertasEstoqueResponse;
import br.com.canteiroconectado.dto.response.ContadoresRequisicaoResponse;
import br.com.canteiroconectado.dto.response.ResumoResponse;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.service.estoque.EstoqueService;
import br.com.canteiroconectado.service.requisicao.RequisicaoService;

@Service
public class ResumoService {

    static final int QUANTOS_CRITICOS = 3;

    private final EstoqueService estoque;
    private final RequisicaoService requisicoes;

    public ResumoService(EstoqueService estoque, RequisicaoService requisicoes) {
        this.estoque = estoque;
        this.requisicoes = requisicoes;
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    public ResumoResponse resumir(UUID canteiroId) {
        AlertasEstoqueResponse alertas = estoque.alertas(canteiroId, QUANTOS_CRITICOS);
        ContadoresRequisicaoResponse contadores = requisicoes.contadores(canteiroId);
        return new ResumoResponse(alertas.zerados(), alertas.abaixoMinimo(), alertas.maisCriticos(),
            contadores.paraDecidir(), contadores.minhas());
    }
}
