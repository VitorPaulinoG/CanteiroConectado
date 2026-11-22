package br.com.canteiroconectado.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class BancoTesteEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPRIEDADE_URL = "canteiro.banco.url";
    static final String SUFIXO = "_test";

    private static final Pattern URL = Pattern.compile("^(jdbc:postgresql://[^/?]+/)([A-Za-z0-9_]+)(\\?.*)?$");
    private static final Set<String> BANCO_JA_EXISTE = Set.of("42P04", "23505");

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 3;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        String url = ambiente.getProperty(PROPRIEDADE_URL);
        if (url == null) {
            return;
        }
        Matcher partes = URL.matcher(url);
        if (!partes.matches() || partes.group(2).endsWith(SUFIXO)) {
            return;
        }
        String banco = partes.group(2) + SUFIXO;
        String parametros = partes.group(3) == null ? "" : partes.group(3);
        criarSeAusente(url, ambiente.getProperty("canteiro.banco.usuario"),
            ambiente.getProperty("canteiro.banco.senha"), banco);
        ambiente.getPropertySources().addFirst(new MapPropertySource("bancoTeste",
            Map.of(PROPRIEDADE_URL, partes.group(1) + banco + parametros)));
    }

    private static void criarSeAusente(String url, String usuario, String senha, String banco) {
        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            if (existe(conexao, banco)) {
                return;
            }
            try (Statement comando = conexao.createStatement()) {
                comando.execute("CREATE DATABASE \"" + banco + "\"");
            }
        } catch (SQLException falha) {
            if (!BANCO_JA_EXISTE.contains(falha.getSQLState())) {
                throw new IllegalStateException("Não foi possível preparar o banco de testes " + banco, falha);
            }
        }
    }

    private static boolean existe(Connection conexao, String banco) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            consulta.setString(1, banco);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }
}
