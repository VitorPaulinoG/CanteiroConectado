package br.com.canteiroconectado.requisicao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pedido de material feito pelo app.
 *
 * @param id UUID gerado no app ao abrir a tela; repetir o envio com o mesmo id não duplica
 * @param canteiroId obrigatório para GESTOR (canteiro ativo); o ALMOXARIFE usa o próprio
 * @param necessarioAte data em que o material precisa estar no canteiro; hoje ou depois
 * @param itens de 1 a 20 itens ativos e distintos
 */
public record RequisicaoRequest(
    String id,

    UUID canteiroId,

    @NotNull(message = "Informe a data de necessidade")
    LocalDate necessarioAte,

    @Size(max = 60, message = "A frente de obra deve ter no máximo 60 caracteres")
    String frenteObra,

    @Size(max = 200, message = "A observação deve ter no máximo 200 caracteres")
    String observacao,

    @NotNull(message = "Informe os itens")
    List<@Valid @NotNull ItemDaRequisicaoRequest> itens
) {
}
