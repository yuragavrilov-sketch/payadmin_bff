package ru.copperside.payadmin.limit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.payadmin.limit.application.LimitManagementUseCase;
import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;

@Configuration(proxyBeanMethods = false)
public class LimitUseCaseConfig {

    @Bean
    LimitManagementUseCase limitManagementUseCase(LimitManagementPort port) {
        return new LimitManagementUseCase(port);
    }
}
