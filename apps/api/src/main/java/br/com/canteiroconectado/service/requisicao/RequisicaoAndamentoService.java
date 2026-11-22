package br.com.canteiroconectado.service.requisicao;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.domain.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.domain.requisicao.Requisicao;
import br.com.canteiroconectado.domain.requisicao.RequisicaoEvento;
import br.com.canteiroconectado.domain.requisicao.StatusRequisicao;
import br.com.canteiroconectado.domain.usuario.Perfil;
import br.com.canteiroconectado.dto.request.AprovarRequisicaoRequest;
import br.com.canteiroconectado.dto.request.RecusarRequisicaoRequest;
import br.com.canteiroconectado.dto.response.RequisicaoResponse;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.repository.requisicao.RequisicaoEventoRepository;
import br.com.canteiroconectado.repository.requisicao.RequisicaoRepository;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.service.auditoria.AuditoriaService;
import br.com.canteiroconectado.service.canteiro.CanteiroAcessoService;
import br.com.canteiroconectado.util.Fuso;

@Service
public class RequisicaoAndamentoService {

    static final int TAMANHO_MINIMO_DO_MOTIVO = 5;

    private final RequisicaoRepository requisicoes;
    private final RequisicaoEventoRepository eventos;
    private final RequisicaoService consulta;
    private final CanteiroAcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public RequisicaoAndamentoService(RequisicaoRepository requisicoes, RequisicaoEventoRepository eventos,
            RequisicaoService consulta, CanteiroAcessoService acesso, AuditoriaService auditoria, Clock relogio) {
        this.requisicoes = requisicoes;
        this.eventos = eventos;
        this.consulta = consulta;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public RequisicaoResponse aprovar(UUID id, AprovarRequisicaoRequest pedido) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        Requisicao requisicao = travarParaDecidir(id, usuario);
        LocalDate previsao = pedido == null ? null : pedido.previsaoChegada();
        if (previsao != null && previsao.isBefore(Fuso.hoje(relogio))) {
            throw new BusinessException(ErrorCode.DATA_NO_PASSADO, "A previsão de chegada não pode ser anterior a hoje.")
                .com("campo", "previsaoChegada");
        }
        String observacao = RequisicaoService.texto(pedido == null ? null : pedido.observacao());
        Map<String, Object> detalhes = detalhes(requisicao);
        if (previsao != null) {
            detalhes.put("previsaoChegada", previsao.toString());
        }
        return mudar(requisicao, StatusRequisicao.APROVADA, usuario, observacao, previsao,
            AcaoAuditoria.REQUISICAO_APROVADA, detalhes);
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public RequisicaoResponse recusar(UUID id, RecusarRequisicaoRequest pedido) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        String motivo = RequisicaoService.texto(pedido == null ? null : pedido.motivo());
        if (motivo == null || motivo.length() < TAMANHO_MINIMO_DO_MOTIVO) {
            throw new BusinessException(ErrorCode.MOTIVO_RECUSA_OBRIGATORIO,
                "Conte o motivo da recusa (pelo menos " + TAMANHO_MINIMO_DO_MOTIVO + " caracteres).")
                .com("campo", "motivo");
        }
        Requisicao requisicao = travarParaDecidir(id, usuario);
        Map<String, Object> detalhes = detalhes(requisicao);
        detalhes.put("motivo", motivo);
        return mudar(requisicao, StatusRequisicao.RECUSADA, usuario, motivo, null, AcaoAuditoria.REQUISICAO_RECUSADA,
            detalhes);
    }

    @PreAuthorize(Permissoes.ALMOXARIFE)
    @Transactional
    public RequisicaoResponse atender(UUID id) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        Requisicao requisicao = travar(id);
        exigirStatus(requisicao, StatusRequisicao.APROVADA, ErrorCode.REQUISICAO_NAO_APROVADA,
            "Só requisições aprovadas podem ser marcadas como atendidas. Esta está "
                + rotulo(requisicao.getStatus()) + ".");
        return mudar(requisicao, StatusRequisicao.ATENDIDA, usuario, null, requisicao.getPrevisaoChegada(),
            AcaoAuditoria.REQUISICAO_ATENDIDA, detalhes(requisicao));
    }

    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional
    public RequisicaoResponse cancelar(UUID id) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        Requisicao requisicao = travar(id);
        if (!requisicao.getSolicitanteId().equals(usuario.id())) {
            throw new BusinessException(ErrorCode.ACESSO_NEGADO, "Só quem criou a requisição pode cancelá-la.");
        }
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {
            String quem = consulta.detalhar(requisicao.getId(), usuario).decididoPorNome();
            String mensagem = requisicao.getStatus() == StatusRequisicao.APROVADA && quem != null
                ? "Esta requisição já foi aprovada por " + quem + ". Para desistir, fale com quem aprovou."
                : "Esta requisição já foi " + rotulo(requisicao.getStatus()) + " e não pode ser cancelada.";
            throw new BusinessException(ErrorCode.REQUISICAO_NAO_PENDENTE, mensagem)
                .com("status", requisicao.getStatus().name());
        }
        return mudar(requisicao, StatusRequisicao.CANCELADA, usuario, null, null, AcaoAuditoria.REQUISICAO_CANCELADA,
            detalhes(requisicao));
    }

    private Requisicao travarParaDecidir(UUID id, UsuarioLogado usuario) {
        Requisicao requisicao = travar(id);
        if (requisicao.getSolicitanteId().equals(usuario.id())) {
            throw new BusinessException(ErrorCode.AUTOAPROVACAO_PROIBIDA,
                "Quem pediu não decide a própria requisição. Ela é decidida pela outra pessoa.");
        }
        if (usuario.perfil() == requisicao.getSolicitantePerfil()) {
            throw new BusinessException(ErrorCode.ACESSO_NEGADO, requisicao.getSolicitantePerfil() == Perfil.GESTOR
                ? "As requisições do gestor são decididas pelo almoxarife."
                : "As requisições do almoxarife são decididas pelo gestor.");
        }
        exigirStatus(requisicao, StatusRequisicao.PENDENTE, ErrorCode.REQUISICAO_NAO_PENDENTE,
            "Esta requisição já foi " + rotulo(requisicao.getStatus()) + " e não pode mais ser decidida.");
        return requisicao;
    }

    Requisicao travar(UUID id) {
        Requisicao requisicao = requisicoes.travar(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Requisição não encontrada."));
        acesso.conferir(requisicao.getCanteiroId());
        return requisicao;
    }

    static void exigirStatus(Requisicao requisicao, StatusRequisicao esperado, ErrorCode codigo, String mensagem) {
        if (requisicao.getStatus() != esperado) {
            throw new BusinessException(codigo, mensagem).com("status", requisicao.getStatus().name());
        }
    }

    RequisicaoResponse mudar(Requisicao requisicao, StatusRequisicao novo, UsuarioLogado usuario, String observacao,
            LocalDate previsao, AcaoAuditoria acao, Map<String, Object> detalhes) {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        if (novo == StatusRequisicao.APROVADA || novo == StatusRequisicao.RECUSADA) {
            requisicao.decidir(novo, usuario.id(), agora, observacao, previsao);
        } else {
            requisicao.setStatus(novo);
        }
        requisicoes.saveAndFlush(requisicao);
        eventos.saveAndFlush(new RequisicaoEvento(requisicao.getId(), novo, usuario.id(), observacao, agora));
        auditoria.registrar(usuario.id(), acao, RequisicaoService.ENTIDADE, requisicao.getId(), detalhes);
        return consulta.detalhar(requisicao.getId(), usuario);
    }

    static Map<String, Object> detalhes(Requisicao requisicao) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("numero", RequisicaoNumeracaoService.formatar(requisicao.getNumero()));
        detalhes.put("canteiroId", requisicao.getCanteiroId().toString());
        return detalhes;
    }

    static String rotulo(StatusRequisicao status) {
        return switch (status) {
            case PENDENTE -> "deixada pendente";
            case APROVADA -> "aprovada";
            case RECUSADA -> "recusada";
            case ATENDIDA -> "atendida";
            case CANCELADA -> "cancelada";
        };
    }
}
