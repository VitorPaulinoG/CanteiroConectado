package br.com.canteiroconectado.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@DisplayName("Cobertura da matriz de permissões")
class CoberturaDaMatrizTest {

    private static final Set<String> PUBLICOS = Set.of("GET /ping", "POST /autenticacao/login");
    private static final String SO_DE_TESTE = "/api/v1/teste-seguranca/";
    private static final Pattern VARIAVEL = Pattern.compile("\\{[^}]+}");
    private static final Pattern OBSERVACAO = Pattern.compile(" \\(.*\\)$");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    private static String normalizar(String endpoint) {
        return VARIAVEL.matcher(OBSERVACAO.matcher(endpoint).replaceAll("")).replaceAll("{}");
    }

    @Test
    @DisplayName("deve ter uma linha na matriz para cada endpoint da API, para nenhum endpoint novo ficar sem teste de acesso")
    void deve_cobrirTodosOsEndpoints_quando_apiTemEndpointNovo() {
        Set<String> daApi = new TreeSet<>();
        mapeamentos.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().stream()
            .filter(caminho -> caminho.startsWith("/api/v1/") && !caminho.startsWith(SO_DE_TESTE))
            .forEach(caminho -> info.getMethodsCondition().getMethods().stream()
                .map(RequestMethod::name)
                .forEach(metodo -> daApi.add(normalizar(metodo + " " + caminho.substring("/api/v1".length()))))));
        Set<String> daMatriz = new TreeSet<>();
        MatrizDePermissoesTest.MATRIZ.forEach(linha -> daMatriz.add(normalizar(linha.endpoint())));

        assertThat(daApi).isNotEmpty();
        assertThat(daApi).allMatch(endpoint -> daMatriz.contains(endpoint) || PUBLICOS.contains(normalizar(endpoint)),
            "todo endpoint está na matriz ou é público");
        assertThat(daMatriz).allMatch(daApi::contains, "toda linha da matriz aponta para um endpoint que existe");
    }
}
