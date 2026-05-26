package ru.copperside.payadmin.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HexagonalArchitectureTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java/ru/copperside/payadmin");

    @Test
    void featureCodeIsPackagedByCapabilityNotHorizontalLayer() throws IOException {
        List<String> forbiddenPackages = List.of("api", "application", "domain", "infrastructure", "config");

        List<String> existingForbiddenPackages = forbiddenPackages.stream()
                .filter(packageName -> Files.isDirectory(MAIN_JAVA.resolve(packageName)))
                .toList();

        assertThat(existingForbiddenPackages).isEmpty();
    }

    @Test
    void merchantDomainAndApplicationDoNotImportAdaptersOrWebInfrastructure() throws IOException {
        List<String> forbiddenImports = sourceFiles(MAIN_JAVA.resolve("merchant"))
                .stream()
                .filter(path -> path.toString().contains("\\domain\\") || path.toString().contains("\\application\\"))
                .flatMap(path -> importsFrom(path).stream())
                .filter(importLine -> importLine.contains(".adapter.")
                        || importLine.contains(".common.web.")
                        || importLine.contains(".security.")
                        || importLine.startsWith("import org.springframework."))
                .toList();

        assertThat(forbiddenImports).isEmpty();
    }

    @Test
    void merchantCapabilityHasExpectedHexagonalPortsAndAdapters() {
        assertThat(Files.isDirectory(MAIN_JAVA.resolve("merchant/domain"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve("merchant/application/port/out"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve("merchant/adapter/in/web"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve("merchant/adapter/out/merchantscore"))).isTrue();
    }

    private List<Path> sourceFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private List<String> importsFrom(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + path, ex);
        }
    }
}

