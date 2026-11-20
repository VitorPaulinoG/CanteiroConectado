package br.com.canteiroconectado.resumo;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import br.com.canteiroconectado.estoque.AlertasDeEstoque;
import br.com.canteiroconectado.estoque.EstoqueService;
import br.com.canteiroconectado.requisicao.ContadoresDeRequisicoes;
import br.com.canteiroconectado.requisicao.RequisicaoService;
import br.com.canteiroconectado.security.Permissoes;

/**
 * Junta os alertas de estoque e os contadores de requisição do canteiro numa resposta só, para o
 * Início carregar com uma chamada.
 */
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
        AlertasDeEstoque alertas = estoque.alertas(canteiroId, QUANTOS_CRITICOS);
        ContadoresDeRequisicoes contadores = requisicoes.contadores(canteiroId);
        return new ResumoResponse(alertas.zerados(), alertas.abaixoDoMinimo(), alertas.maisCriticos(),
            contadores.paraDecidir(), contadores.minhas());
    }
}
