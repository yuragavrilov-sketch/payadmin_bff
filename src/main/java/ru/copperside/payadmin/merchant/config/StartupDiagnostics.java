package ru.copperside.payadmin.merchant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;
    private final MerchantsCoreProperties merchantsCoreProperties;
    private final PayadminMerchantsProperties payadminMerchantsProperties;

    public StartupDiagnostics(
            Environment environment,
            MerchantsCoreProperties merchantsCoreProperties,
            PayadminMerchantsProperties payadminMerchantsProperties
    ) {
        this.environment = environment;
        this.merchantsCoreProperties = merchantsCoreProperties;
        this.payadminMerchantsProperties = payadminMerchantsProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "payadmin-bff startup: profiles={}, merchantsCoreBaseUrl={}, connectTimeout={}, readTimeout={}, pageSize={}, maxPages={}, internalAdminKeyConfigured={}, unknownMcc={}",
                Arrays.toString(environment.getActiveProfiles()),
                merchantsCoreProperties.baseUrl(),
                merchantsCoreProperties.connectTimeout(),
                merchantsCoreProperties.readTimeout(),
                merchantsCoreProperties.pageSize(),
                merchantsCoreProperties.maxPages(),
                !merchantsCoreProperties.internalAdminApiKey().isBlank(),
                payadminMerchantsProperties.unknownMcc()
        );
    }
}

