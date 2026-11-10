package br.com.canteiroconectado.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Registra cada SQL preparado pela aplicação, seja pelo Hibernate, seja pelo {@code JdbcTemplate},
 * para os testes de N+1 contarem quantas consultas uma requisição faz.
 */
@TestComponent
public class ContadorDeConsultas implements BeanPostProcessor {

    private final List<String> consultas = new CopyOnWriteArrayList<>();
    private volatile boolean contando;

    @Override
    public Object postProcessAfterInitialization(Object bean, String nome) {
        if (bean instanceof DataSource fonte && !(bean instanceof FonteContada)) {
            return new FonteContada(fonte);
        }
        return bean;
    }

    public void iniciar() {
        consultas.clear();
        contando = true;
    }

    public List<String> parar() {
        contando = false;
        return List.copyOf(consultas);
    }

    private final class FonteContada extends DelegatingDataSource {

        FonteContada(DataSource alvo) {
            super(alvo);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return contada(super.getConnection());
        }

        @Override
        public Connection getConnection(String usuario, String senha) throws SQLException {
            return contada(super.getConnection(usuario, senha));
        }

        private Connection contada(Connection conexao) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, metodo, argumentos) -> {
                    if (contando && argumentos != null && argumentos.length > 0 && argumentos[0] instanceof String sql
                            && (metodo.getName().equals("prepareStatement") || metodo.getName().equals("prepareCall"))) {
                        consultas.add(sql);
                    }
                    try {
                        return metodo.invoke(conexao, argumentos);
                    } catch (InvocationTargetException falha) {
                        throw falha.getCause();
                    }
                });
        }
    }
}
