package br.com.canteiroconectado.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.canteiroconectado.autenticacao.PoliticaDeSenha;
import br.com.canteiroconectado.usuario.AdminInitializer;

/**
 * Carrega os dados de demonstração na subida, só quando o perfil {@code demo} está ativo
 * ({@code SPRING_PROFILES_ACTIVE=local,demo} ou {@code prod,demo}). Roda depois do
 * {@link AdminInitializer} e só em banco sem o canteiro de demonstração. A senha dos usuários de
 * demonstração vem de {@code DEMO_SENHA}, obrigatória neste perfil, e não fica no repositório.
 */
@Component
@Profile("demo")
@Order(AdminInitializer.ORDEM + 10)
public class DemoDataInitializer implements ApplicationRunner {

    private final CargaDeDemonstracao carga;
    private final String senha;

    public DemoDataInitializer(CargaDeDemonstracao carga, @Value("${canteiro.demo.senha:}") String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                "O perfil demo exige a variável de ambiente DEMO_SENHA (senha de Sebastião e Camila).");
        }
        PoliticaDeSenha.problemaDa(senha).ifPresent(problema -> {
            throw new IllegalStateException("A variável de ambiente DEMO_SENHA não atende à política de senha: "
                + problema);
        });
        this.carga = carga;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        carga.carregar(senha);
    }
}
