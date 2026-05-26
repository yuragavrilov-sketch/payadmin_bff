package ru.copperside.payadmin.merchant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.payadmin.merchant.application.ListMerchantsUseCase;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;

@Configuration(proxyBeanMethods = false)
public class MerchantUseCaseConfig {

    @Bean
    ListMerchantsUseCase listMerchantsUseCase(MerchantCatalogPort merchantCatalogPort) {
        return new ListMerchantsUseCase(merchantCatalogPort);
    }
}
