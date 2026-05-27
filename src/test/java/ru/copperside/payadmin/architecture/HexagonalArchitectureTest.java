package ru.copperside.payadmin.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    void domainAndApplicationDoNotImportAdaptersOrWebInfrastructure() throws IOException {
        List<String> forbiddenImports = new ArrayList<>();
        for (String capability : List.of("merchant", "limit", "terminal")) {
            forbiddenImports.addAll(forbiddenBoundaryImports(capability));
        }

        assertThat(forbiddenImports).isEmpty();
    }

    @Test
    void capabilitiesHaveExpectedHexagonalPortsAndAdapters() {
        assertCapabilityStructure("merchant", "merchantscore");
        assertCapabilityStructure("limit", "limitmanagement");
        assertCapabilityStructure("terminal", "merchantscore");
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

    private List<String> forbiddenBoundaryImports(String capability) throws IOException {
        return sourceFiles(MAIN_JAVA.resolve(capability))
                .stream()
                .filter(this::isDomainOrApplicationSource)
                .flatMap(path -> importsFrom(path).stream())
                .filter(importLine -> importLine.contains(".adapter.")
                        || importLine.contains(".common.web.")
                        || importLine.contains(".security.")
                        || importLine.startsWith("import org.springframework."))
                .toList();
    }

    private boolean isDomainOrApplicationSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/domain/") || normalized.contains("/application/");
    }

    private void assertCapabilityStructure(String capability, String outboundAdapter) {
        assertThat(Files.isDirectory(MAIN_JAVA.resolve(capability + "/domain"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve(capability + "/application/port/out"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve(capability + "/adapter/in/web"))).isTrue();
        assertThat(Files.isDirectory(MAIN_JAVA.resolve(capability + "/adapter/out/" + outboundAdapter))).isTrue();
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

