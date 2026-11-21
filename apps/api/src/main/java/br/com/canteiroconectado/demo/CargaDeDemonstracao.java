package br.com.canteiroconectado.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.canteiroconectado.autenticacao.AutenticacaoService;
import br.com.canteiroconectado.autenticacao.TrocarSenhaRequest;
import br.com.canteiroconectado.canteiro.CanteiroRepository;
import br.com.canteiroconectado.canteiro.CanteiroRequest;
import br.com.canteiroconectado.canteiro.CanteiroService;
import br.com.canteiroconectado.colaborador.ColaboradorRequest;
import br.com.canteiroconectado.colaborador.ColaboradorService;
import br.com.canteiroconectado.comum.Fuso;
import br.com.canteiroconectado.demo.ComoUsuario.Pessoa;
import br.com.canteiroconectado.estoque.EstornoRequest;
import br.com.canteiroconectado.estoque.EstornoService;
import br.com.canteiroconectado.estoque.MotivoMovimentacao;
import br.com.canteiroconectado.estoque.MovimentacaoRequest;
import br.com.canteiroconectado.estoque.RegistroDeMovimentacaoService;
import br.com.canteiroconectado.estoque.TipoMovimentacao;
import br.com.canteiroconectado.item.ItemRequest;
import br.com.canteiroconectado.item.ItemService;
import br.com.canteiroconectado.item.TipoItem;
import br.com.canteiroconectado.item.Unidade;
import br.com.canteiroconectado.requisicao.ItemDaRequisicaoRequest;
import br.com.canteiroconectado.requisicao.RequisicaoRequest;
import br.com.canteiroconectado.requisicao.RequisicaoService;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;
import br.com.canteiroconectado.usuario.UsuarioRequest;
import br.com.canteiroconectado.usuario.UsuarioResponse;
import br.com.canteiroconectado.usuario.UsuarioService;

/**
 * Monta o cenário das duas jornadas do produto pelos mesmos services que o app usa: o
 * administrador cria os canteiros e os funcionários, o almoxarife Sebastião cadastra itens e
 * colaboradores e registra as entradas e saídas, e a gestora Camila fica com uma requisição para
 * decidir. Os saldos nascem só de movimentações. Tudo roda numa transação: se uma etapa falhar,
 * nada fica gravado.
 *
 * <p>Resultado: o canteiro {@value #CANTEIRO_PRINCIPAL} com 22 itens, o "Cimento CP-II 50kg" abaixo
 * do mínimo (Jornada 2) e 2 furadeiras disponíveis (Jornada 1).</p>
 */
@Component
public class CargaDeDemonstracao {

    private static final Logger LOG = LoggerFactory.getLogger(CargaDeDemonstracao.class);

    public static final String CANTEIRO_PRINCIPAL = "Residencial Aurora";
    public static final String CANTEIRO_SECUNDARIO = "Edifício Horizonte";
    public static final String EMAIL_ALMOXARIFE = "sebastiao@canteiro.demo";
    public static final String EMAIL_GESTORA = "camila@canteiro.demo";

    record ItemDaCarga(String codigo, String descricao, Unidade unidade, TipoItem tipo, String minimo,
            String recebido) {
    }

    static final List<ItemDaCarga> ITENS = List.of(
        new ItemDaCarga("MAT-0001", "Cimento CP-II 50kg", Unidade.SACO, TipoItem.MATERIAL, "50", "30"),
        new ItemDaCarga("MAT-0002", "Areia média", Unidade.M3, TipoItem.MATERIAL, "5", "12"),
        new ItemDaCarga("MAT-0003", "Brita 1", Unidade.M3, TipoItem.MATERIAL, "5", "10"),
        new ItemDaCarga("MAT-0004", "Argamassa AC-III 20kg", Unidade.SACO, TipoItem.MATERIAL, "20", "15"),
        new ItemDaCarga("MAT-0005", "Tijolo cerâmico 9 furos", Unidade.UN, TipoItem.MATERIAL, "1000", "4000"),
        new ItemDaCarga("MAT-0006", "Bloco de concreto 14x19x39", Unidade.UN, TipoItem.MATERIAL, "500", "300"),
        new ItemDaCarga("MAT-0007", "Vergalhão CA-50 10mm", Unidade.UN, TipoItem.MATERIAL, "50", "120"),
        new ItemDaCarga("MAT-0008", "Arame recozido 18", Unidade.KG, TipoItem.MATERIAL, "10", "25"),
        new ItemDaCarga("MAT-0009", "Cal hidratada 20kg", Unidade.SACO, TipoItem.MATERIAL, "10", null),
        new ItemDaCarga("MAT-0010", "Tinta acrílica branca 18L", Unidade.LATA, TipoItem.MATERIAL, "4", "6"),
        new ItemDaCarga("MAT-0011", "Prego 17x21", Unidade.KG, TipoItem.MATERIAL, "5", "8"),
        new ItemDaCarga("MAT-0012", "Tubo PVC esgoto 100mm", Unidade.M, TipoItem.MATERIAL, "30", "60"),
        new ItemDaCarga("MAT-0013", "Impermeabilizante 18L", Unidade.LATA, TipoItem.MATERIAL, "2", "3"),
        new ItemDaCarga("MAT-0014", "Luva de raspa (par)", Unidade.UN, TipoItem.MATERIAL, "20", "40"),
        new ItemDaCarga("FER-0012", "Furadeira de impacto 700W", Unidade.UN, TipoItem.FERRAMENTA, "1", "3"),
        new ItemDaCarga("FER-0013", "Martelete rompedor 10kg", Unidade.UN, TipoItem.FERRAMENTA, "1", "1"),
        new ItemDaCarga("FER-0014", "Serra circular 7 1/4", Unidade.UN, TipoItem.FERRAMENTA, "1", "2"),
        new ItemDaCarga("FER-0015", "Esmerilhadeira 4 1/2", Unidade.UN, TipoItem.FERRAMENTA, "1", "2"),
        new ItemDaCarga("FER-0016", "Betoneira 400L", Unidade.UN, TipoItem.FERRAMENTA, "0", "1"),
        new ItemDaCarga("FER-0017", "Nível a laser", Unidade.UN, TipoItem.FERRAMENTA, "0", "1"),
        new ItemDaCarga("FER-0018", "Carrinho de mão", Unidade.UN, TipoItem.FERRAMENTA, "2", "4"),
        new ItemDaCarga("FER-0019", "Escada extensível 7m", Unidade.UN, TipoItem.FERRAMENTA, "1", "2"));

    private final UsuarioRepository usuarios;
    private final CanteiroRepository canteiroRepository;
    private final CanteiroService canteiros;
    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacao;
    private final ItemService itens;
    private final ColaboradorService colaboradores;
    private final RegistroDeMovimentacaoService movimentacoes;
    private final EstornoService estornos;
    private final RequisicaoService requisicoes;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    public CargaDeDemonstracao(UsuarioRepository usuarios, CanteiroRepository canteiroRepository,
            CanteiroService canteiros, UsuarioService usuarioService, AutenticacaoService autenticacao,
            ItemService itens, ColaboradorService colaboradores, RegistroDeMovimentacaoService movimentacoes,
            EstornoService estornos, RequisicaoService requisicoes, Clock relogio,
            PlatformTransactionManager transacoes) {
        this.usuarios = usuarios;
        this.canteiroRepository = canteiroRepository;
        this.canteiros = canteiros;
        this.usuarioService = usuarioService;
        this.autenticacao = autenticacao;
        this.itens = itens;
        this.colaboradores = colaboradores;
        this.movimentacoes = movimentacoes;
        this.estornos = estornos;
        this.requisicoes = requisicoes;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(transacoes);
    }

    /**
     * Carrega os dados se o canteiro principal ainda não existe.
     *
     * @param senha senha de Sebastião e Camila (vinda de {@code DEMO_SENHA})
     * @return verdadeiro quando a carga foi feita agora; falso quando os dados já existiam
     */
    public boolean carregar(String senha) {
        if (canteiroRepository.existsByNomeIgnoreCase(CANTEIRO_PRINCIPAL)) {
            LOG.info("Dados de demonstração já existem; nada a carregar");
            return false;
        }
        transacao.executeWithoutResult(status -> montar(senha));
        LOG.info("Dados de demonstração carregados: {} itens no canteiro {}", ITENS.size(), CANTEIRO_PRINCIPAL);
        return true;
    }

    private void montar(String senha) {
        Usuario admin = usuarios.findFirstByAdministradorTrueOrderByCriadoEmAsc()
            .orElseThrow(() -> new IllegalStateException("A carga de demonstração precisa do administrador."));
        Pessoa administrador = new Pessoa(admin.getId(), Perfil.GESTOR, true, null);

        UUID aurora = ComoUsuario.executar(administrador, () -> canteiros.criar(
            new CanteiroRequest(CANTEIRO_PRINCIPAL, "Rua das Palmeiras, 120 - Centro")).id());
        ComoUsuario.executar(administrador, () -> canteiros.criar(
            new CanteiroRequest(CANTEIRO_SECUNDARIO, "Avenida Brasil, 2500 - Jardim América")));

        Pessoa sebastiao = funcionario(administrador, "Sebastião Ferreira", EMAIL_ALMOXARIFE, Perfil.ALMOXARIFE,
            aurora, senha);
        funcionario(administrador, "Camila Rocha", EMAIL_GESTORA, Perfil.GESTOR, null, senha);

        Map<String, UUID> item = new LinkedHashMap<>();
        Map<String, UUID> colaborador = new LinkedHashMap<>();
        ComoUsuario.executar(sebastiao, () -> {
            for (ItemDaCarga dados : ITENS) {
                item.put(dados.codigo(), itens.criar(new ItemRequest(dados.codigo(), dados.descricao(),
                    dados.unidade(), dados.tipo(), new BigDecimal(dados.minimo()))).id());
            }
            for (String nome : List.of("João da Silva", "Maria Oliveira", "Pedro Santos", "Ana Costa")) {
                colaborador.put(nome, colaboradores.criar(new ColaboradorRequest(nome, aurora)).id());
            }

            for (ItemDaCarga dados : ITENS) {
                if (dados.recebido() != null) {
                    entrada(aurora, item.get(dados.codigo()), dados.recebido(), "Recebimento inicial da obra");
                }
            }
            saida(aurora, item.get("MAT-0002"), "4", colaborador.get("Pedro Santos"), "Fundação");
            saida(aurora, item.get("MAT-0004"), "15", colaborador.get("Maria Oliveira"), "Alvenaria 2º andar");
            saida(aurora, item.get("MAT-0005"), "1500", colaborador.get("Maria Oliveira"), "Alvenaria 2º andar");
            saida(aurora, item.get("MAT-0014"), "8", colaborador.get("Ana Costa"), "Equipe de alvenaria");
            saida(aurora, item.get("FER-0012"), "1", colaborador.get("João da Silva"), "Instalações elétricas");
            saida(aurora, item.get("FER-0013"), "1", colaborador.get("João da Silva"), "Demolição da casa antiga");
            saida(aurora, item.get("FER-0015"), "1", colaborador.get("Pedro Santos"), "Estrutura");
            movimentacoes.registrar(aurora, new MovimentacaoRequest(UUID.randomUUID().toString(),
                TipoMovimentacao.SAIDA, MotivoMovimentacao.AJUSTE, item.get("MAT-0011"), new BigDecimal("1"), null,
                null, "Pacote molhado na chuva"));

            UUID lancadaEmDobro = entrada(aurora, item.get("MAT-0003"), "2", "NF 3381 lançada em dobro");
            estornos.estornar(lancadaEmDobro, new EstornoRequest(UUID.randomUUID().toString(),
                "Nota fiscal já tinha sido lançada"));

            requisicoes.criar(new RequisicaoRequest(UUID.randomUUID().toString(), aurora,
                Fuso.hoje(relogio).plusDays(5), "Alvenaria 2º andar", "Para a próxima semana",
                List.of(new ItemDaRequisicaoRequest(item.get("MAT-0006"), new BigDecimal("500")),
                    new ItemDaRequisicaoRequest(item.get("MAT-0004"), new BigDecimal("30")))));
            return null;
        });
    }

    private Pessoa funcionario(Pessoa administrador, String nome, String email, Perfil perfil, UUID canteiroId,
            String senha) {
        UsuarioResponse criado = ComoUsuario.executar(administrador,
            () -> usuarioService.criar(new UsuarioRequest(nome, email, perfil, canteiroId)));
        String temporaria = ComoUsuario.executar(administrador,
            () -> usuarioService.credencialInicial(criado.id()).senhaTemporaria());
        autenticacao.trocarSenha(criado.id(), new TrocarSenhaRequest(temporaria, senha));
        return new Pessoa(criado.id(), perfil, false, canteiroId);
    }

    private UUID entrada(UUID canteiroId, UUID itemId, String quantidade, String observacao) {
        return movimentacoes.registrar(canteiroId, new MovimentacaoRequest(UUID.randomUUID().toString(),
            TipoMovimentacao.ENTRADA, MotivoMovimentacao.RECEBIMENTO, itemId, new BigDecimal(quantidade), null, null,
            observacao)).registro().id();
    }

    private void saida(UUID canteiroId, UUID itemId, String quantidade, UUID colaboradorId, String frente) {
        movimentacoes.registrar(canteiroId, new MovimentacaoRequest(UUID.randomUUID().toString(),
            TipoMovimentacao.SAIDA, MotivoMovimentacao.RETIRADA, itemId, new BigDecimal(quantidade), colaboradorId,
            frente, null));
    }
}
