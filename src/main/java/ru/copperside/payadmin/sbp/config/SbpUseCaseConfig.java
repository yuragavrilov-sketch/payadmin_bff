package ru.copperside.payadmin.sbp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.sbp.application.traffic.SbpTrafficUseCase;
import ru.copperside.payadmin.sbp.application.traffic.port.out.SbpTrafficPort;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class SbpUseCaseConfig {

    @Bean
    SbpTrafficUseCase sbpTrafficUseCase(SbpTrafficPort port) {
        return new SbpTrafficUseCase(port);
    }

    @Bean
    RestClient sbpRestClient(SbpRouterManagementProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
