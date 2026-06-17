package ru.copperside.payadmin.crossborder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.payadmin.crossborder.application.CrossBorderQueries;
import ru.copperside.payadmin.crossborder.application.port.out.CrossBorderEnginePort;

@Configuration(proxyBeanMethods = false)
public class CrossBorderUseCaseConfig {

    @Bean
    CrossBorderQueries crossBorderQueries(CrossBorderEnginePort port) {
        return new CrossBorderQueries(port);
    }
}
