package com.virtus.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching  // ✅ QUESTA È LA RIGA MANCANTE!
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    System.out.println("🗑️ CACHE REMOVAL: " + key + " - Causa: " + cause);
                }));

        // ✅ Specifica i nomi delle cache
        cacheManager.setCacheNames(Arrays.asList("orari-allenamenti", "librebooking-auth","prenotazioni-per-palestra"));

        return cacheManager;
    }
}