package com.virtus.service;

import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrariAllenamentiService {

    private final WebClient webClient;
    private final LibreBookingProperties properties;

    @Qualifier("simpleRestTemplate")
    private final RestTemplate restTemplate;

    // Cache in memoria
    private LibreBookingAuthResponse currentAuth;
    private LocalDateTime lastAuthTime;
    private static final Object AUTH_LOCK = new Object();
    private static final int AUTH_VALIDITY_MINUTES = 25;

    @PostConstruct
    public void init() {
        // Pre-warm della connessione all'avvio
        prewarmConnection();
    }

    /**
     * Pre-riscalda la connessione TCP all'avvio per ridurre latenza prima chiamata
     */
    private void prewarmConnection() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    properties.getBaseUrl(),
                    HttpMethod.HEAD,
                    entity,
                    String.class
            );
            log.info("Connection pre-warmed successfully");
        } catch (Exception e) {
            // Non critico, continua comunque
        }
    }

    @Cacheable(value = "orari-allenamenti", key = "'all'")
    public List<OrarioAllenamentoDto> getOrariAllenamenti() {
        return getOrariAllenamentiWithRetry(3);
    }

    private List<OrarioAllenamentoDto> getOrariAllenamentiWithRetry(int maxRetries) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 1. Autentica
                LibreBookingAuthResponse auth = authenticate();

                // 2. Recupera reservations
                List<LibreBookingReservationDto> reservations = getReservations(auth, 2, null);

                // 3. Filtra per la settimana target e trasforma
                List<OrarioAllenamentoDto> mappedResults = reservations.stream()
                        .filter(this::isInTargetWeek)
                        .map(this::mapToOrarioAllenamento)
                        .collect(Collectors.toList());

                return mappedResults;

            } catch (Exception e) {
                lastException = e;
                log.error("Attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(attempt * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All {} attempts failed. Returning fallback data. Last error: {}",
                maxRetries, lastException != null ? lastException.getMessage() : "Unknown error");
        return getFallbackData();
    }

    @Cacheable(value = "prenotazioni-per-palestra", key = "#nomePalestra")
    public List<OrarioAllenamentoDto> getPrenotazioniForPalestra(String nomePalestra) {
        try {
            LibreBookingAuthResponse auth = authenticate();
            List<LibreBookingReservationDto> reservations = getReservations(auth, 4, nomePalestra);

            return reservations.stream()
                    .filter(this::isInTargetWeek)
                    .map(this::mapToOrarioAllenamento)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error retrieving data from LibreBooking: {}", e.getMessage());
            return getFallbackData();
        }
    }

    /**
     * Determina se una prenotazione appartiene alla settimana target
     */
    private boolean isInTargetWeek(LibreBookingReservationDto reservation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetWeekStart = getTargetWeekStart(now);
        LocalDateTime targetWeekEnd = targetWeekStart.plusDays(6).withHour(23).withMinute(59).withSecond(59);

        LocalDateTime reservationStart = convertToLocalTimezone(reservation.getStartDate());

        return !reservationStart.isBefore(targetWeekStart) &&
                !reservationStart.isAfter(targetWeekEnd);
    }

    /**
     * Calcola l'inizio della settimana target basandosi sulla logica richiesta
     */
    private LocalDateTime getTargetWeekStart(LocalDateTime now) {
        DayOfWeek currentDay = now.getDayOfWeek();

        if (currentDay == DayOfWeek.SATURDAY && now.getHour() >= 8) {
            // Sabato dopo le 8:00 -> settimana successiva
            return now.plusDays(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Tutti gli altri casi -> settimana corrente
            int daysToSubtract = currentDay.getValue() - DayOfWeek.MONDAY.getValue();
            return now.minusDays(daysToSubtract)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    /**
     * Sistema di autenticazione con cache
     */
    private LibreBookingAuthResponse authenticate() {
        if (isAuthValid()) {
            return currentAuth;
        }

        synchronized (AUTH_LOCK) {
            // Double-check pattern
            if (isAuthValid()) {
                return currentAuth;
            }

            return performAuthentication();
        }
    }

    /**
     * Esegue l'autenticazione effettiva con retry
     */
    private LibreBookingAuthResponse performAuthentication() {
        Exception lastException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                LibreBookingAuthRequest request = LibreBookingAuthRequest.builder()
                        .username(properties.getCredentials().getUsername())
                        .password(properties.getCredentials().getPassword())
                        .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.add("Connection", "keep-alive");

                HttpEntity<LibreBookingAuthRequest> entity = new HttpEntity<>(request, headers);
                String authUrl = properties.getBaseUrl() + properties.getAuthEndpoint();

                ResponseEntity<LibreBookingAuthResponse> response = restTemplate.exchange(
                        authUrl,
                        HttpMethod.POST,
                        entity,
                        LibreBookingAuthResponse.class
                );

                if (response.getBody() != null && response.getBody().getSessionToken() != null) {
                    currentAuth = response.getBody();
                    lastAuthTime = LocalDateTime.now();
                    return currentAuth;
                } else {
                    throw new RuntimeException("Invalid authentication response: missing token");
                }

            } catch (Exception e) {
                lastException = e;

                if (attempt < 3) {
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Authentication failed after 3 attempts", lastException);
    }

    /**
     * Verifica se l'autenticazione cached è ancora valida
     */
    private boolean isAuthValid() {
        return currentAuth != null &&
                lastAuthTime != null &&
                lastAuthTime.plusMinutes(AUTH_VALIDITY_MINUTES).isAfter(LocalDateTime.now());
    }

    /**
     * Pre-autentica in background (opzionale)
     */
    @Async
    public void preAuthenticate() {
        if (!isAuthValid()) {
            try {
                authenticate();
            } catch (Exception e) {
                log.warn("Pre-authentication failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Forza il refresh dell'autenticazione
     */
    public void forceAuthRefresh() {
        synchronized (AUTH_LOCK) {
            currentAuth = null;
            lastAuthTime = null;
            authenticate();
        }
    }

    /**
     * Recupera le prenotazioni dal servizio LibreBooking
     */
    private List<LibreBookingReservationDto> getReservations(LibreBookingAuthResponse auth, long weeks, String resourceNameFilter) {
        LocalDateTime startDate = getMondayOfCurrentWeek();
        LocalDateTime endDate = startDate.plusWeeks(weeks);

        String url = String.format("%s%s?startDateTime=%s&endDateTime=%s",
                properties.getBaseUrl(),
                properties.getReservationsEndpoint(),
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        try {
            LibreBookingReservationsResponse response = webClient.get()
                    .uri(url)
                    .header("X-Booked-SessionToken", auth.getSessionToken())
                    .header("X-Booked-UserId", auth.getUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(LibreBookingReservationsResponse.class)
                    .block();

            List<LibreBookingReservationDto> allReservations = response != null ?
                    response.getReservations() : List.of();

            // Filtra le prenotazioni che non richiedono approvazione
            allReservations = allReservations.stream()
                    .filter(res -> !res.isRequiresApproval())
                    .toList();

            // Filtra per resourceName se specificato
            if (resourceNameFilter != null && !resourceNameFilter.isBlank()) {
                return allReservations.stream()
                        .filter(res -> resourceNameFilter.equalsIgnoreCase(res.getResourceName()))
                        .toList();
            }

            return allReservations;

        } catch (Exception e) {
            log.error("Error communicating with LibreBooking: {}", e.getMessage());
            throw new RuntimeException("LibreBooking communication error: " + e.getMessage(), e);
        }
    }

    /**
     * Mappa una prenotazione LibreBooking in OrarioAllenamentoDto
     */
    private OrarioAllenamentoDto mapToOrarioAllenamento(LibreBookingReservationDto reservation) {
        LocalDateTime startDateTime = convertToLocalTimezone(reservation.getStartDate());
        LocalDateTime endDateTime = convertToLocalTimezone(reservation.getEndDate());

        return OrarioAllenamentoDto.builder()
                .gruppo(reservation.getFirstName() + " " + reservation.getLastName())
                .giorno(getGiornoSettimana(startDateTime))
                .orario(formatOrario(startDateTime, endDateTime))
                .palestra(reservation.getResourceName())
                .palestraId(reservation.getResourceId())
                .referenceNumber(reservation.getReferenceNumber())
                .title(reservation.getTitle())
                .description(reservation.getDescription())
                .startDate(startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .endDate(endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isRecurring(reservation.isRecurring())
                .build();
    }

    /**
     * Converte una data/ora dal formato dell'API al fuso orario locale
     */
    private LocalDateTime convertToLocalTimezone(String dateTimeString) {
        try {
            String cleanDateTime = dateTimeString.replaceAll("[+-]\\d{4}$", "").trim();

            LocalDateTime parsedDateTime;
            try {
                parsedDateTime = LocalDateTime.parse(cleanDateTime);
            } catch (DateTimeParseException e) {
                parsedDateTime = LocalDateTime.parse(cleanDateTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            // Determina il fuso orario di origine
            ZoneId sourceZone;
            if (dateTimeString.contains("+0000") || dateTimeString.endsWith("Z")) {
                sourceZone = ZoneId.of("UTC");
            } else if (dateTimeString.contains("+0100")) {
                sourceZone = ZoneId.of("+01:00");
            } else if (dateTimeString.contains("+0200")) {
                sourceZone = ZoneId.of("+02:00");
            } else {
                sourceZone = ZoneId.of("UTC");
            }

            // Converti al fuso orario target (Europe/Rome)
            ZoneId targetZone = ZoneId.of("Europe/Rome");
            ZonedDateTime sourceZonedDateTime = parsedDateTime.atZone(sourceZone);
            ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);

            return targetZonedDateTime.toLocalDateTime();

        } catch (Exception e) {
            log.error("Error converting timezone for '{}': {}", dateTimeString, e.getMessage());
            return LocalDateTime.parse(dateTimeString.replace("+0000", "").trim());
        }
    }

    private String getGiornoSettimana(LocalDateTime dateTime) {
        String[] giorni = {"Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"};
        return giorni[dateTime.getDayOfWeek().getValue() % 7];
    }

    private String formatOrario(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + "-" + end.format(formatter);
    }

    private List<OrarioAllenamentoDto> getFallbackData() {
        return List.of(
                OrarioAllenamentoDto.builder()
                        .gruppo("Under 19")
                        .giorno("Lunedì")
                        .orario("18:00-20:00")
                        .palestra("Palestra A")
                        .build()
        );
    }

    @CacheEvict(value = "orari-allenamenti", allEntries = true)
    public List<OrarioAllenamentoDto> refreshOrariAllenamenti() {
        return getOrariAllenamenti();
    }

    /**
     * Ottiene il lunedì della settimana corrente alle 00:00
     */
    private LocalDateTime getMondayOfCurrentWeek() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        int daysToSubtract = currentDay.getValue() - DayOfWeek.MONDAY.getValue();

        return now.minusDays(daysToSubtract)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}