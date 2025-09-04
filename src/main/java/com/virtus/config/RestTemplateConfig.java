package com.virtus.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
/*
    @Bean
    public RestTemplate libreBookingRestTemplate() {
        // Connection pooling manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100); // Max connessioni totali
        connectionManager.setDefaultMaxPerRoute(20); // Max connessioni per route
        connectionManager.setValidateAfterInactivity(1000); // Valida dopo 1s di inattività

        // Request configuration con timeout aggressivi
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(2000) // 2s per ottenere connessione dal pool
                .setConnectTimeout(3000) // 3s per stabilire connessione TCP
                .setSocketTimeout(5000) // 5s per socket read timeout
                .build();

        // Build del client HTTP con pooling
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections() // Rimuovi connessioni scadute automaticamente
                .evictIdleConnections(30, java.util.concurrent.TimeUnit.SECONDS) // Rimuovi idle dopo 30s
                .setConnectionManagerShared(false) // Connection manager dedicato
                .build();

        // Factory per RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient((HttpClient) httpClient);
        factory.setConnectTimeout(3000); // 3 secondi
        factory.setReadTimeout(5000); // 5 secondi
        factory.setConnectionRequestTimeout(2000); // 2 secondi

        return new RestTemplate(factory);
    }
*/
    /**
     * RestTemplate alternativo usando RestTemplateBuilder di Spring Boot
     * (più semplice ma con meno controllo)
     */
    @Bean
    public RestTemplate simpleRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}