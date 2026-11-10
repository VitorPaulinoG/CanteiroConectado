package br.com.canteiroconectado.requisicao;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.comum.IdDoCliente;
import br.com.canteiroconectado.comum.PaginaResponse;
import br.com.canteiroconectado.comum.Paginacao;
import br.com.canteiroconectado.comum.Quantidades;
import br.com.canteiroconectado.comum.ResultadoDaGravacao;
import br.com.canteiroconectado.exception.BusinessException;
import br.com.canteiroconectado.exception.ErrorCode;
import br.com.canteiroconectado.item.ItemService;
import br.com.canteiroconectado.security.CanteiroAcessoService;
import br.com.canteiroconectado.security.Permissoes;
import br.com.canteiroconectado.security.UsuarioLogado;
import br.com.canteiroconectado.usuario.Perfil;

/**
 * Criação e consulta de requisições pelo ALMOXARIFE (no próprio canteiro) e pelo GESTOR (no
 * canteiro ativo). O {@code id} vem do app: o mesmo pedido enviado de novo devolve a requisição já
 * criada.
 */
@Service
public class RequisicaoService {

    static final String ENTIDADE = "requisicao";
    static final int MAXIMO_DE_ITENS = 20;

    private final RequisicaoRepository requisicoes;
    private final RequisicaoEventoRepository eventos;
    private final RequisicaoConsultaRepository consultas;
    private final NumeracaoDeRequisicao numeracao;
    private final CanteiroService canteiros;
    private final ItemService itens;
    private final CanteiroAcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    private final TransactionTemplate escrita;
    private final TransactionTemplate leitura;

    public RequisicaoService(RequisicaoRepository requisicoes, RequisicaoEventoRepository eventos,
            RequisicaoConsultaRepository consultas, NumeracaoDeRequisicao numeracao, CanteiroService canteiros,
            ItemService itens, CanteiroAcessoService acesso, AuditoriaService auditoria, Clock relogio,
            PlatformTransactionManager transacoes) {
        this.requisicoes = requisicoes;
        this.eventos = eventos;
        this.consultas = consultas;
        this.numeracao = numeracao;
        this.canteiros = canteiros;
        this.itens = itens;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.escrita = new TransactionTemplate(transacoes);
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
    }

    record PedidoNormalizado(UUID id, UUID canteiroId, UUID solicitanteId, LocalDate necessarioAte, String frenteObra,
            String observacao, List<ItemDaRequisicaoRequest> itens) {
    }

    /**
     * Cria a requisição como {@code PENDENTE}, com o próximo número do canteiro, ou devolve a já
     * criada com o mesmo id.
     *
     * @throws BusinessException {@code ID_INVALIDO}, {@code ACESSO_NEGADO}, {@code CANTEIRO_INATIVO},
     *     {@code REQUISICAO_SEM_ITENS}, {@code LIMITE_DE_ITENS}, {@code ITEM_REPETIDO},
     *     {@code QUANTIDADE_INVALIDA}, {@code ITEM_INATIVO}, {@code DATA_NO_PASSADO} ou
     *     {@code ID_JA_UTILIZADO}
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    public ResultadoDaGravacao<RequisicaoResponse> criar(RequisicaoRequest requisicao) {
        UUID id = IdDoCliente.ler(requisicao.id());
        UsuarioLogado usuario = UsuarioLogado.atual();
        UUID canteiroId = acesso.resolver(requisicao.canteiroId());
        PedidoNormalizado pedido = new PedidoNormalizado(id, canteiroId, usuario.id(), requisicao.necessarioAte(),
            texto(requisicao.frenteObra()), texto(requisicao.observacao()), requisicao.itens());

        if (Boolean.TRUE.equals(leitura.execute(status -> jaCriada(pedido)))) {
            return ResultadoDaGravacao.repetido(detalhar(id, usuario));
        }
        validar(pedido);
        try {
            boolean criadaAgora = Boolean.TRUE.equals(escrita.execute(status -> {
                if (jaCriada(pedido)) {
                    return false;
                }
                gravar(pedido, usuario);
                return true;
            }));
            return new ResultadoDaGravacao<>(detalhar(id, usuario), criadaAgora);
        } catch (DataIntegrityViolationException corrida) {
            if (Boolean.TRUE.equals(leitura.execute(status -> jaCriada(pedido)))) {
                return ResultadoDaGravacao.repetido(detalhar(id, usuario));
            }
            throw corrida;
        }
    }

    private void gravar(PedidoNormalizado pedido, UsuarioLogado usuario) {
        canteiros.exigirAtivo(pedido.canteiroId());
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Requisicao requisicao = new Requisicao(pedido.id(), numeracao.proximo(pedido.canteiroId()), pedido.canteiroId(),
            usuario.id(), usuario.perfil(), pedido.frenteObra(), pedido.observacao(), pedido.necessarioAte(), agora);
        for (ItemDaRequisicaoRequest item : pedido.itens()) {
            requisicao.adicionarItem(item.itemId(), Quantidades.validar(item.quantidade(), "quantidade"));
        }
        requisicoes.saveAndFlush(requisicao);
        eventos.saveAndFlush(new RequisicaoEvento(requisicao.getId(), StatusRequisicao.PENDENTE, usuario.id(), null, agora));
        auditoria.registrar(usuario.id(), AcaoAuditoria.REQUISICAO_CRIADA, ENTIDADE, requisicao.getId(),
            Map.of("numero", NumeracaoDeRequisicao.formatar(requisicao.getNumero()),
                "canteiroId", pedido.canteiroId().toString(), "itens", pedido.itens().size()));
    }

    private void validar(PedidoNormalizado pedido) {
        List<ItemDaRequisicaoRequest> lista = pedido.itens();
        if (lista.isEmpty()) {
            throw new BusinessException(ErrorCode.REQUISICAO_SEM_ITENS, "Adicione pelo menos um item.")
                .com("campo", "itens");
        }
        if (lista.size() > MAXIMO_DE_ITENS) {
            throw new BusinessException(ErrorCode.LIMITE_DE_ITENS,
                "Uma requisição aceita no máximo " + MAXIMO_DE_ITENS + " itens.").com("campo", "itens");
        }
        Set<UUID> vistos = new HashSet<>();
        for (int indice = 0; indice < lista.size(); indice++) {
            ItemDaRequisicaoRequest item = lista.get(indice);
            String prefixo = "itens[" + indice + "].";
            if (!vistos.add(item.itemId())) {
                throw new BusinessException(ErrorCode.ITEM_REPETIDO,
                    "O mesmo item aparece duas vezes. Junte as quantidades numa linha só.")
                    .com("campo", prefixo + "itemId");
            }
            Quantidades.validar(item.quantidade(), prefixo + "quantidade");
            try {
                itens.exigirAtivo(item.itemId());
            } catch (BusinessException recusa) {
                throw recusa.com("campo", prefixo + "itemId");
            }
        }
        if (pedido.necessarioAte().isBefore(Fuso.hoje(relogio))) {
            throw new BusinessException(ErrorCode.DATA_NO_PASSADO, "A data de necessidade não pode ser anterior a hoje.")
                .com("campo", "necessarioAte");
        }
    }

    private boolean jaCriada(PedidoNormalizado pedido) {
        Optional<Requisicao> existente = requisicoes.findById(pedido.id());
        if (existente.isEmpty()) {
            return false;
        }
        if (!mesmoConteudo(existente.get(), pedido)) {
            throw new BusinessException(ErrorCode.ID_JA_UTILIZADO,
                "Este id já foi usado em outra requisição. Gere um id novo para um pedido novo.").com("campo", "id");
        }
        return true;
    }

    /**
     * Diz se a requisição gravada é o mesmo pedido: mesmos canteiro, autor, prazo, frente,
     * observação e itens com as mesmas quantidades.
     */
    static boolean mesmoConteudo(Requisicao existente, PedidoNormalizado pedido) {
        Map<UUID, BigDecimal> gravados = new HashMap<>();
        existente.getItens().forEach(item -> gravados.put(item.getItemId(), item.getQuantidade()));
        Map<UUID, BigDecimal> pedidos = new HashMap<>();
        for (ItemDaRequisicaoRequest item : pedido.itens()) {
            if (item.quantidade() == null || pedidos.put(item.itemId(), item.quantidade()) != null) {
                return false;
            }
        }
        boolean mesmosItens = gravados.size() == pedidos.size() && gravados.entrySet().stream()
            .allMatch(gravado -> pedidos.containsKey(gravado.getKey())
                && pedidos.get(gravado.getKey()).compareTo(gravado.getValue()) == 0);
        return mesmosItens
            && existente.getCanteiroId().equals(pedido.canteiroId())
            && existente.getSolicitanteId().equals(pedido.solicitanteId())
            && existente.getNecessarioAte().equals(pedido.necessarioAte())
            && Objects.equals(existente.getFrenteObra(), pedido.frenteObra())
            && Objects.equals(existente.getObservacao(), pedido.observacao());
    }

    /**
     * Lista as requisições do canteiro na aba pedida, com os pendentes primeiro.
     *
     * @param canteiroId canteiro ativo; o ALMOXARIFE pode omitir
     * @param status filtro opcional de status
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public PaginaResponse<RequisicaoResumoResponse> listar(UUID canteiroId, String escopo,
            List<StatusRequisicao> status, Integer pagina, Integer tamanho) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        FiltroDeRequisicoes filtro = new FiltroDeRequisicoes(acesso.resolver(canteiroId), usuario.id(),
            perfilQueDecide(usuario), EscopoDeRequisicoes.ler(escopo), status == null ? List.of() : List.copyOf(status));
        Pageable paginacao = Paginacao.de(pagina, tamanho);
        long total = consultas.contar(filtro);
        int totalPaginas = (int) Math.ceil((double) total / paginacao.getPageSize());
        return new PaginaResponse<>(consultas.listar(filtro, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao.getPageNumber(), paginacao.getPageSize(), total, totalPaginas);
    }

    /**
     * Requisição completa com itens, linha do tempo e as ações permitidas ao usuário logado.
     *
     * @throws BusinessException {@code RECURSO_NAO_ENCONTRADO} ou {@code ACESSO_NEGADO}
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public RequisicaoResponse buscar(UUID id) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        RequisicaoResponse requisicao = detalhar(id, usuario);
        acesso.conferir(requisicao.canteiroId());
        return requisicao;
    }

    /**
     * Contadores do Início: quantas requisições o usuário pode decidir no canteiro e quantas dele
     * estão em cada status.
     */
    @PreAuthorize(Permissoes.ALMOXARIFE_OU_GESTOR)
    @Transactional(readOnly = true)
    public ContadoresDeRequisicoes contadores(UUID canteiroId) {
        UsuarioLogado usuario = UsuarioLogado.atual();
        acesso.conferir(canteiroId);
        long paraDecidir = consultas.contar(new FiltroDeRequisicoes(canteiroId, usuario.id(), perfilQueDecide(usuario),
            EscopoDeRequisicoes.PARA_DECIDIR, List.of()));
        return new ContadoresDeRequisicoes(paraDecidir, consultas.contarDoUsuarioPorStatus(canteiroId, usuario.id()));
    }

    /**
     * Perfil cujas requisições o usuário decide: o ALMOXARIFE decide as do GESTOR e o GESTOR as do
     * ALMOXARIFE.
     */
    static Perfil perfilQueDecide(UsuarioLogado usuario) {
        return usuario.isAlmoxarife() ? Perfil.GESTOR : Perfil.ALMOXARIFE;
    }

    RequisicaoResponse detalhar(UUID id, UsuarioLogado usuario) {
        RequisicaoResponse requisicao = consultas.buscar(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECURSO_NAO_ENCONTRADO, "Requisição não encontrada."));
        return requisicao.comAcoes(AcoesDaRequisicao.para(usuario, requisicao.solicitanteId(),
            requisicao.solicitantePerfil(), requisicao.status()));
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
