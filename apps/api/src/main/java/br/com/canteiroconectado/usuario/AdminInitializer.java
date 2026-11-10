package br.com.canteiroconectado.usuario;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.canteiroconectado.auditoria.AcaoAuditoria;
import br.com.canteiroconectado.auditoria.AuditoriaService;
import br.com.canteiroconectado.config.AdminProperties;

/**
 * Cria o GESTOR administrador na subida da API a partir de {@code ADMIN_NOME}, {@code ADMIN_EMAIL}
 * e {@code ADMIN_SENHA}, se ainda não houver nenhum administrador. Numa segunda subida nada muda.
 * O administrador nasce com {@code precisa_trocar_senha}, mas para ele a troca é opcional.
 */
@Component
@Order(AdminInitializer.ORDEM)
public class AdminInitializer implements ApplicationRunner {

    public static final int ORDEM = 0;

    private static final Logger LOG = LoggerFactory.getLogger(AdminInitializer.class);

    static final String ENTIDADE = "usuario";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoderSenha;
    private final AuditoriaService auditoria;
    private final AdminProperties propriedades;

    public AdminInitializer(UsuarioRepository usuarios, PasswordEncoder encoderSenha,
            AuditoriaService auditoria, AdminProperties propriedades) {
        this.usuarios = usuarios;
        this.encoderSenha = encoderSenha;
        this.auditoria = auditoria;
        this.propriedades = propriedades;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.existsByAdministradorTrue()) {
            LOG.debug("Administrador já existe; nada a criar");
            return;
        }
        String email = Usuario.normalizarEmail(propriedades.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Não há administrador, mas o e-mail de ADMIN_EMAIL (" + email
                + ") já pertence a outro usuário. Use outro e-mail em ADMIN_EMAIL.");
        }

        Usuario admin = new Usuario(propriedades.nome().strip(), email,
            encoderSenha.encode(propriedades.senha()), Perfil.GESTOR, true, null);
        admin.setPrecisaTrocarSenha(true);
        usuarios.save(admin);

        auditoria.registrar(null, AcaoAuditoria.ADMIN_CRIADO, ENTIDADE, admin.getId(),
            Map.of("email", email));
        LOG.info("Administrador criado: {}", email);
    }
}
