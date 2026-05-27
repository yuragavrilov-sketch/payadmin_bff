package ru.copperside.payadmin.terminal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.payadmin.terminal.application.ListTerminalsUseCase;
import ru.copperside.payadmin.terminal.application.port.out.TerminalCatalogPort;

@Configuration(proxyBeanMethods = false)
public class TerminalUseCaseConfig {

    @Bean
    ListTerminalsUseCase listTerminalsUseCase(TerminalCatalogPort terminalCatalogPort) {
        return new ListTerminalsUseCase(terminalCatalogPort);
    }
}
