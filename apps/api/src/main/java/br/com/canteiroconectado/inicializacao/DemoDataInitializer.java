package br.com.canteiroconectado.inicializacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.canteiroconectado.util.SenhaValidator;

@Component
@Profile("demo")
@Order(AdminInitializer.ORDEM + 10)
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataLoader carga;
    private final String senha;

    public DemoDataInitializer(DemoDataLoader carga, @Value("${canteiro.demo.senha:}") String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                "O perfil demo exige a variável de ambiente DEMO_SENHA (senha de Sebastião e Camila).");
        }
        SenhaValidator.problema(senha).ifPresent(problema -> {
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
