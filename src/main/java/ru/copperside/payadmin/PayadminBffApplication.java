package ru.copperside.payadmin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@OpenAPIDefinition(info = @Info(title = "payadmin-bff", version = "v1"))
@SpringBootApplication
@ConfigurationPropertiesScan
public class PayadminBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayadminBffApplication.class, args);
    }
}

