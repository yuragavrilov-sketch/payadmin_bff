package ru.copperside.payadmin.limitprojection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.payadmin.limitprojection.application.ReservationProjectionUseCase;
import ru.copperside.payadmin.limitprojection.application.port.out.ReservationProjectionPort;

@Configuration(proxyBeanMethods = false)
public class LimitProjectionUseCaseConfig {

    @Bean
    ReservationProjectionUseCase reservationProjectionUseCase(ReservationProjectionPort port) {
        return new ReservationProjectionUseCase(port);
    }
}
