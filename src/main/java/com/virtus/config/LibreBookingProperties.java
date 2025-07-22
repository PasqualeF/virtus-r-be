
// ========== CONFIGURAZIONI ==========
package com.virtus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "librebooking")
@Data
public class LibreBookingProperties {

    private String baseUrl;
    private String authEndpoint;
    private String reservationsEndpoint;
    private String accountsEndpoint;
    private Credentials credentials = new Credentials();
    private Cache cache = new Cache();
    private DateRange dateRange = new DateRange();

    @Data
    public static class Credentials {
        private String username;
        private String password;
    }

    @Data
    public static class Cache {
        private int timeoutMinutes = 30;
    }

    @Data
    public static class DateRange {
        private int defaultDaysAhead = 14;
    }
}




