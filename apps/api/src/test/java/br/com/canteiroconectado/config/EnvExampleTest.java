package br.com.canteiroconectado.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Contrato do .env.example")
class EnvExampleTest {

    private static final Pattern VARIAVEL_NO_YAML = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[:}]");
    private static final List<String> ARQUIVOS_YAML =
        List.of("application.yml", "application-local.yml", "application-prod.yml");

    @Nested
    @DisplayName("variáveis lidas pela API")
    class VariaveisLidasPelaApi {

        @Test
        @DisplayName("deve documentar no .env.example toda variável lida nos arquivos application*.yml")
        void deve_documentarNoEnvExample_quando_apiLeVariavel() throws IOException {
            Set<String> lidas = new TreeSet<>();
            for (String arquivo : ARQUIVOS_YAML) {
                try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(arquivo)) {
                    assertThat(entrada).as(arquivo).isNotNull();
                    Matcher casador = VARIAVEL_NO_YAML.matcher(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
                    while (casador.find()) {
                        lidas.add(casador.group(1));
                    }
                }
            }

            Set<String> documentadas = new TreeSet<>();
            for (String linha : Files.readAllLines(localizarEnvExample())) {
                if (!linha.isBlank() && !linha.startsWith("#") && linha.contains("=")) {
                    documentadas.add(linha.substring(0, linha.indexOf('=')).trim());
                }
            }

            assertThat(lidas).isNotEmpty();
            assertThat(documentadas).containsAll(lidas);
        }
    }

    private static Path localizarEnvExample() {
        for (Path pasta = Path.of("").toAbsolutePath(); pasta != null; pasta = pasta.getParent()) {
            Path candidato = pasta.resolve(".env.example");
            if (Files.exists(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException(".env.example não encontrado a partir de " + Path.of("").toAbsolutePath());
    }
}
