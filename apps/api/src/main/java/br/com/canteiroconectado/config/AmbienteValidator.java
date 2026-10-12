package br.com.canteiroconectado.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

public class AmbienteValidator implements EnvironmentPostProcessor, Ordered {

    private static final Map<String, Class<?>> PROPRIEDADES = new LinkedHashMap<>();

    static {
        PROPRIEDADES.put("canteiro.banco", DbProperties.class);
        PROPRIEDADES.put("canteiro.jwt", JwtProperties.class);
        PROPRIEDADES.put("canteiro.admin", AdminProperties.class);
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        Set<String> problemas = new TreeSet<>();
        Binder binder = Binder.get(ambiente);
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator validador = new SpringValidatorAdapter(fabrica.getValidator());
            PROPRIEDADES.forEach((prefixo, tipo) -> {
                try {
                    binder.bind(prefixo, Bindable.of(tipo), new ValidationBindHandler(validador));
                } catch (BindException falha) {
                    problemas.addAll(descrever(prefixo, falha));
                }
            });
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException(montarMensagem(problemas));
        }
    }

    private static Set<String> descrever(String prefixo, BindException falha) {
        Set<String> mensagens = new TreeSet<>();
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            if (causa instanceof BindValidationException validacao) {
                for (ObjectError erro : validacao.getValidationErrors().getAllErrors()) {
                    mensagens.add(erro.getDefaultMessage());
                }
            }
        }
        if (mensagens.isEmpty()) {
            mensagens.add("Valor inválido em " + falha.getName() + " (" + prefixo + ")");
        }
        return mensagens;
    }

    private static String montarMensagem(Set<String> problemas) {
        StringBuilder mensagem = new StringBuilder("A API não pode iniciar: configuração de ambiente inválida.");
        problemas.forEach(problema -> mensagem.append("\n - ").append(problema));
        return mensagem.toString();
    }
}
